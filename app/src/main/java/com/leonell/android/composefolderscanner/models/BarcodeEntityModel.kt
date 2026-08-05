package com.leonell.android.composefolderscanner.models

import com.google.gson.JsonElement
import com.leonell.android.composefolderscanner.database.model.FolderLocationEntity


data class Locations(
    val locations : ArrayList<BarcodeEntityModel.FolderLocation> = arrayListOf()
)

data class BarcodeLookup(
   val barcode : String,
   // Nullable: Gson leaves this null when the server omits the field, and a
   // non-null Kotlin type does not stop that -- it just crashes later.
   val entityType : FolderEntityType?,
   val entity : JsonElement,
   var entityObject : Any?
)

sealed class BarcodeEntityModel() {
   data class StaffAssignment (
      val id: Number,
      val associateId: Number,
      var fullName: String,
      val staffId: Number,
      val staffName: String?,
      val procurementAreaId: Number,
      val procurementAreaName: String,
      val type: String,
      val canEdit: Boolean,
   ) : BarcodeEntityModel()

   data class FolderLocation (
      val id: Number,
      var name: String,
      var original: String?,
      var spaceA : String? = "",
      var spaceB : String? = "",
      var spaceC	: String? = "",
      var filtered: Boolean = false,
      var favorite:  Boolean  = false,
      var custom : Boolean  = false,
      val folderEntityType : FolderEntityType? = FolderEntityType.LOCATION
   ) : BarcodeEntityModel()

   data class Person (
      val id : Number?,
      val persId : String?,
      val associateId : String?,
      val addoId : Number,
      val fullName : String? = "Unknown Name",
      val name : String? = "Search Name",
      val deceased : Boolean,
      val celebrity : Boolean,
      val deadfiled : Boolean,
      val executive : Boolean?,
      val inCf : Boolean,
      val caseLevel : String? = "No Case Level Data",
      val trainingLevel : String? = "No Training Level Data",
      val dateOfBirth : Number?,
      val address : String?,
      val country : String?,
      val order : Number?,
      val standingId : Number?,
      val standingName : String?,
      val label : String?,
      val emailAddress : String?,
      val homePhone : String?,
      val workPhone : String?,
      val altPhone : String?,
      var barcodeId: String?,
      var individualStatus : String?,
      var language : String?,
      var procurementCategory : String?,
      var mailable : Boolean?,
      val folders: List<FolderModel>?
   ): BarcodeEntityModel() {
      fun getFoldersTypes(): List<String> {
         val distinctTypes : ArrayList<String> = arrayListOf()
         folders?.forEach {
            distinctTypes.add(it.folderSeriesName)
         }
         return distinctTypes.distinct()
      }
      fun filterFolderTypes(folderType : String) = folders?.filter { it.folderSeriesName == folderType }
   }
}

fun BarcodeEntityModel.FolderLocation.asDatabaseModel() =
         FolderLocationEntity(
            id = id.toLong(),
            name = name,
            original = original,
            spaceA = spaceA,
            spaceB = spaceB,
            spaceC = spaceC,
            filtered =  filtered,
            favorite = favorite,
            custom = custom,
            folderEntityType = FolderEntityType.LOCATION
         )

