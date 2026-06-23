"""Import normalized real-estate policy events into DuckDB.

The import is idempotent: a policy event is identified by its normalized name
and analysis date, then updated in place when it already exists.
"""

import argparse
import csv
from datetime import datetime
from pathlib import Path
from typing import Any

from app.services.duckdb_service import DEFAULT_DB, get_connection, init_schema


ROOT_DIR = Path(__file__).resolve().parents[1]
DEFAULT_CSV = ROOT_DIR / "seed" / "policy_events.csv"
EVENT_TYPE = "policy"


def _validate_date(value: str, field: str) -> str:
    try:
        return datetime.strptime(value, "%Y-%m-%d").date().isoformat()
    except ValueError as exc:
        raise ValueError(f"{field} must be YYYY-MM-DD: {value}") from exc


def _event_ym(event_date: str) -> str:
    return event_date.replace("-", "")[:6]


def _next_event_id(con: Any) -> int:
    return int(con.execute("SELECT COALESCE(MAX(id), 0) + 1 FROM events").fetchone()[0])


def _drop_source_url_column(con: Any) -> bool:
    exists = con.execute(
        """
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'main'
          AND table_name = 'policy_event_details'
          AND column_name = 'source_url'
        """
    ).fetchone()
    if exists:
        con.execute("ALTER TABLE policy_event_details DROP COLUMN source_url")
        return True
    return False


def _existing_event_id(con: Any, name: str, event_date: str) -> int | None:
    row = con.execute(
        """
        SELECT id
        FROM events
        WHERE name = ?
          AND event_type = ?
          AND event_date = CAST(? AS DATE)
        """,
        [name, EVENT_TYPE, event_date],
    ).fetchone()
    return int(row[0]) if row else None


def import_policy_events(csv_path: Path, db_path: Path) -> dict[str, int | bool]:
    init_schema(db_path)
    con = get_connection(db_path)
    inserted = 0
    updated = 0

    con.execute("BEGIN TRANSACTION")
    try:
        source_url_removed = _drop_source_url_column(con)
        with csv_path.open("r", encoding="utf-8-sig", newline="") as file:
            for row in csv.DictReader(file):
                event_date = _validate_date(row["event_date"], "event_date")
                announced_date = _validate_date(row["announced_date"], "announced_date")
                effective_date = _validate_date(row["effective_date"], "effective_date")
                event_id = _existing_event_id(con, row["name"].strip(), event_date)

                if event_id is None:
                    event_id = _next_event_id(con)
                    con.execute(
                        """
                        INSERT INTO events (id, name, event_type, event_date, event_ym, source, description)
                        VALUES (?, ?, ?, CAST(? AS DATE), ?, ?, ?)
                        """,
                        [event_id, row["name"].strip(), EVENT_TYPE, event_date, _event_ym(event_date), row["source"].strip(), row["description"].strip()],
                    )
                    inserted += 1
                else:
                    con.execute(
                        """
                        UPDATE events
                        SET event_ym = ?, source = ?, description = ?
                        WHERE id = ?
                        """,
                        [_event_ym(event_date), row["source"].strip(), row["description"].strip(), event_id],
                    )
                    con.execute("DELETE FROM policy_event_details WHERE event_id = ?", [event_id])
                    updated += 1

                con.execute(
                    """
                    INSERT INTO policy_event_details (
                        event_id, policy_category, policy_direction, target_region,
                        affected_asset, announced_date, effective_date
                    )
                    VALUES (?, ?, ?, ?, ?, CAST(? AS DATE), CAST(? AS DATE))
                    """,
                    [
                        event_id,
                        row["policy_category"].strip(),
                        row["policy_direction"].strip(),
                        row["target_region"].strip(),
                        row["affected_asset"].strip(),
                        announced_date,
                        effective_date,
                    ],
                )
        con.execute("COMMIT")
    except Exception:
        con.execute("ROLLBACK")
        raise

    return {"inserted": inserted, "updated": updated, "source_url_removed": source_url_removed}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", default=str(DEFAULT_CSV))
    parser.add_argument("--db", default=str(DEFAULT_DB))
    args = parser.parse_args()
    result = import_policy_events(Path(args.csv), Path(args.db))
    print(result)


if __name__ == "__main__":
    main()
