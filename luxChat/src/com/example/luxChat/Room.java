package com.example.luxChat;

public class Room {

	public int roomId;
	public int unread = 0;
	public String sellerEmail;
	public String buyerEmail;
	public int productSeq;
	public String brandName;
	public String sellerName;
	public String buyerName;
	public String sellerReadDate;
	public String buyerReadDate;

	public Room() {
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public String getSellerEmail() {
		return sellerEmail;
	}

	public void setSellerEmail(String sellerEmail) {
		this.sellerEmail = sellerEmail;
	}

	public String getBuyerEmail() {
		return buyerEmail;
	}

	public void setBuyerEmail(String buyerEmail) {
		this.buyerEmail = buyerEmail;
	}

	public int getProductSeq() {
		return productSeq;
	}

	public void setProductSeq(int productSeq) {
		this.productSeq = productSeq;
	}

	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName(String sellerName) {
		this.sellerName = sellerName;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public String getSellerReadDate() {
		return sellerReadDate;
	}

	public void setSellerReadDate(String sellerReadDate) {
		this.sellerReadDate = sellerReadDate;
	}

	public String getBuyerReadDate() {
		return buyerReadDate;
	}

	public void setBuyerReadDate(String buyerReadDate) {
		this.buyerReadDate = buyerReadDate;
	}
}
