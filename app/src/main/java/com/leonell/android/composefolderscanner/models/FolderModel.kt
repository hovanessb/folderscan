package com.leonell.android.composefolderscanner.models

enum class  FolderEntityType {
   FOLDER, LOCATION, SHELF, UNKNOWN
}


data class FolderModel (
   val id : Number?=0,
   var barcode :  String? = "",
   var givenName : String= "",
   val familyName : String= "",
   val maidenName : String= "",
   val fullName : String? = "",
   val currentLocationId : Number? = 0,
   val currentLocationName : String= "",
   val currentLocationFullName : String= "",
   val shelfA : String = "",
   val shelfB : String = "",
   val shelfC : String = "",
   val currentOrgId : Number = 1,
   val currentOrgName : String = "FSO",
   val currentLocationDate : Number? = 0,
   val currentParentId : Number? = 0,
   val currentParentName : String?= "",
   val folderNumber : String? = "",
   var folderSeriesId : String = "00",
   val folderSeriesName : String = "",
   val folderTypeId: Number =0,
   val folderTypeName : String? = "",
   val fromDate: Number? =0,
   val toDate: Number? = 0,
   val loggedBy : String = "",
   val warehouseLocationId : Number? = 0,
   val warehouseLocationName : String? = "",
   val warehouseParentId : Number? = 0,
   val persId : String? = "",
   var addoId : Number? = 0,
   val folderEntityType : FolderEntityType = FolderEntityType.FOLDER,
   // Plain values, not Compose MutableState. Holding UI-toolkit state inside a data model
   // meant the reader thread mutated snapshot state directly, and the model could not be
   // used outside a composition.
   val rssi : Float = 0.0F,
   val phase : Float = 0.0F,
   val count : Float = 0.0F,
)