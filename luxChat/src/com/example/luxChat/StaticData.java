package com.example.luxChat;

/**
 * Url 및 Static 관리용 클래스.
 */
public class StaticData {

	public static final String LOG_TAG = "luxChat";

	public static final String USER_AGENT = "WebChatClient/0.1 (Android)";

//	public static final String HTTP_CHAT_URL = "http://114.200.199.6/luxChat";
//	public static final String HTTP_CHAT_URL = "http://10.0.2.2:8080/luxChat";
	public static final String HTTP_CHAT_URL = "http://www.etalk.co.kr:8081/luxChat";

	// 로그인..
	public static final String URL_LOGON = HTTP_CHAT_URL + "/logon";

	// 로그아웃
	public static final String URL_LOGOUT = HTTP_CHAT_URL + "/logout";

	// 채팅서버에 접속해서, 다른 사용자의 채팅 메시지를 수신하기 위한 연결 url
	public static final String URL_CHAT_ON = HTTP_CHAT_URL + "/chaton";

	// 채팅방 만들기(신규 채팅 신청시 사용함)
	public static final String URL_CREATE_ROOM = HTTP_CHAT_URL + "/create";

	// 채팅 메시지 보내기.
	public static final String URL_SEND_CHAT = HTTP_CHAT_URL + "/sendchat";

	// 채팅 중 사진 보내기.
	public static final String URL_SEND_PHOTO = HTTP_CHAT_URL + "/sendphoto";

	// 채팅방에 포함된 사용자의 채팅 메시지 읽은 시간 요청
	public static final String URL_READ_STATUS = HTTP_CHAT_URL + "/readstatus";

	// 최근 메시지 가져오기.
	public static final String URL_LATEST = HTTP_CHAT_URL + "/latest";

	// 채팅방 목록 가져오기.
	public static final String URL_CRLIST = HTTP_CHAT_URL + "/crlist";

	// 모든 채팅방과 채팅방의 메시지 가져오기.
	public static final String URL_ALL_LIST = HTTP_CHAT_URL + "/getAllMsgList";

	// 채팅방 및 메시지 삭제 처리
	public static final String URL_CHAT_ROOM_DELETE = HTTP_CHAT_URL + "/delete";

	// 채팅방 번호에 해당하는 채팅방 정보를 가져온다.
	public static final String URL_GET_CHAT_ROOM = HTTP_CHAT_URL + "/getRoom";

	// 채팅방에 진입할 때 해당 방의 메시지 읽은 시간을 업데이트 하기 위한 URL - 채팅방에 진입할 때 마다 요청한다.
	public static final String URL_UPDATE_READ_STATUS = HTTP_CHAT_URL + "/updateReadStatus";
}
