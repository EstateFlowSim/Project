import { driver } from 'driver.js'
import 'driver.js/dist/driver.css'

const TOUR_KEY = 'estateflow-analysis-tour-done'

export function useAnalysisTour() {
  function startTour() {
    let d!: ReturnType<typeof driver>

    d = driver({
      animate: true,
      smoothScroll: false,
      allowClose: true,
      overlayOpacity: 0,
      popoverClass: 'ef-tour-popover',
      nextBtnText: '다음 →',
      prevBtnText: '이전',
      doneBtnText: '완료',
      showProgress: true,
      steps: [
        {
          element: '.clseg.clev',
          popover: {
            title: '이벤트 선택',
            description:
              '분석할 부동산 시장 이벤트를 클릭해 선택하세요. 연도 화살표로 다른 연도로 이동할 수 있습니다.',
            side: 'bottom',
            align: 'start',
          },
        },
        {
          element: '.clwin',
          popover: {
            title: '관측 기간 설정',
            description:
              '이벤트 이후 몇 개월을 분석할지 설정합니다. 3M · 6M · 12M 중 선택하세요.',
            side: 'bottom',
            align: 'start',
          },
        },
        {
          element: '.bot',
          popover: {
            title: '타임라인 조작',
            description:
              '슬라이더를 드래그하거나 재생 버튼으로 이벤트 전후 시점을 탐색합니다. T+0이 이벤트 발생 시점입니다.',
            side: 'top',
            align: 'center',
          },
        },
        {
          element: '.rp-wrap',
          popover: {
            title: '지표 패널',
            description:
              '현재 시점 기준 최대 변화율과 반응 지역 수를 확인합니다. ＋/－ 버튼으로 패널 크기를 조절할 수 있습니다.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '.report-btn',
          popover: {
            title: 'AI 리포트 생성',
            description:
              '분석 완료 후 AI 리포트를 생성합니다. 이벤트 영향 분석과 지역별 가격 변동 인사이트를 PDF로 받아볼 수 있습니다.',
            side: 'bottom',
            align: 'end',
          },
        },
      ],

      onDestroyStarted: () => {
        localStorage.setItem(TOUR_KEY, '1')
        d.destroy()
      },
    })

    d.drive()
  }

  function startTourIfFirst() {
    if (!localStorage.getItem(TOUR_KEY)) {
      startTour()
    }
  }

  return { startTour, startTourIfFirst }
}
