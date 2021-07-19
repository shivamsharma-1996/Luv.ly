package com.shivam.guftagoo.models

data class User(
    var uid: String= "",
    var imageUrl:String = "",
    var countryCode: String = "",
    var phoneNumber: String = "",
    var name: String = "",
    var dob: String = "",
    var gender: String = "",
    var videos: List<String> = ArrayList(),
    var interestList:  List<String> = ArrayList()
)