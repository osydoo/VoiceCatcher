# VoiceCatcher
1>. 전체 시나리오
 (1). 어플을 실행 시 각 arcore, 음성인식, 얼굴인식이 작업을 수행할 준비를 한다.
 (2). Google cloud speech to text를 이용한 작업을 수행하여 실시간으로 음성을 인식하고 텍스트화 한다.
 (3). Arcamera의 frame을 image로 변환한 후 MLKit를 이용한 작업을 수행하여 실시간으로 카메라가 얼굴을 인식하고 있는 상태인지 확인한다.
 (4). 얼굴이 인식되지 않은 상황에서는 텍스트화 된 음성이 리스트뷰에 저장된다.
 (5). 얼굴이 인식된 상황에서는 텍스트화 된 음성이 말풍선에 담겨 증강현실로 표현된다.
 (6). 각각의 작업들이 병렬적으로 반복수행 된다.

2>. 추가 기능
 (1). 상단 버튼을 통하여 증강현실로 띄울 말풍선의 디자인을 선택할 수 있다.
 (2). 카메라에 얼굴이 잡히지 않을 때에도 모든 음성들은 리스트뷰에 저장되어 언제든지 확인할 수 있다.
 (3). clear버튼을 통하여 띄워져 있는 AR오브젝트들을 삭제할 수 있다.

 (1) 구현기능

 어플이 실행되면 우선 UI_Task를 실행하여 카메라 프레임을 ARFragment에 올려질 때 까지 대기 합니다.

 ARFragment가 준비가 됐다면 Face_Thread Runnable 쓰레드를 반복적으로 수행합니다.

 이 쓰레드는 내부에서 findFace클래스 객체를 호출하고 findFace클래스에서는 카메라 프레임을 이미지형식으로 변환하여 firebase네트워킹을 사용한 MLKit FaceDetect를 통해 이미지에서 얼굴이 있는지, 있다면 몇 개의 얼굴을 인식했는지 등을 확인합니다.

 이 때 Face_Thread Runnable의 수행속도가 음성인식 쓰레드의 수행속도보다 빠르기 때문에 쓰레드간의 동기화를 맞춰주기 위해 LOCK변수를 사용하였습니다. MLKit FaceDetect 결과에 따라 onSuccess리스너 함수 또는 onFailure를 호출하여 성공 시 인식한 얼굴의 좌표값을 전역변수로 저장합니다.

 Face_Thread Runnable 쓰레드의 실행중에 병렬적으로 SPEECH_API 안에 있는 Speech Service Listener가 실행되는데,
이 리스너는 Google Cloud Speech To Text를 통해 음성을 인식하고 동시에 findFace클래스를 통해 반환된 얼굴의 인식여부인 faceSuccess에 따라 텍스트화 된 음성을 AR로 표현하기 위해서  Rendering 함수를 수행할지에 대한 여부를 결정합니다. 

 Rendering함수에서 앞서 얻은 얼굴의 좌표값을 벡터값인 inAirpose에 저장합니다. inAirpose는 화면상의 좌표와 현실세계 좌표를 조합하여 rendering된 객체를  띄울 위치를 저장하는 변수입니다. rendering방식은 ARCore-Sceneform의 ViewRenderable 클래스를 사용하여 텍스트 뷰 자체를 rendering하는 방식을 선택하였습니다. 이 때 세 가지의 말풍선 종류 버튼의 클릭 여부에 따라 전역변수를 수정함으로써 해당 말풍선 디자인을 갖는 텍스트 뷰를 rendering 하도록 하였습니다.

 이제 텍스트화 된 음성과 inAirpose에 저장된 좌표, ViewRenderable 이 세 가지를 조합하여 AR객체를 화면상에 띄워줍니다. 이러한 얼굴인식, 음성인식, rendering 의 일련의 과정을 매 프레임별로 반복수행합니다.

7. 개발환경
 Windows10, JDK1.8.0_144, Android Studio 3.1.3, Android SDK 27, Samsung S8 Android Studio ARCore, ML Kit, Google Cloud Speech To Text
