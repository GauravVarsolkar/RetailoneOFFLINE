package com.retailone.pos.models.LoginModels

data class NoticeResponse(
    val status: Int,
    val code: String,
    val message: String,
    val resultDt: String,
    val notices: List<Notice>
)

data class Notice(
    val noticeNo: Int,
    val title: String,
    val content: String,
    val detailUrl: String?,
    val regName: String?,
    val regDate: String
)