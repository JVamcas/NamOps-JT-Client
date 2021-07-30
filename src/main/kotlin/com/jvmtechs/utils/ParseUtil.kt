package com.jvmtechs.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jvmtechs.controllers.AbstractView
import com.jvmtechs.model.AbstractModel
import com.jvmtechs.model.User
import com.jvmtechs.model.UserGroup
import com.jvmtechs.utils.DateUtil.Companion._24
import com.jvmtechs.utils.DateUtil.Companion.localDateTimeToday
import com.jvmtechs.utils.ParseUtil.Companion.listToString
import com.jvmtechs.utils.ParseUtil.Companion.localDateFormat
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import jxl.write.Label
import jxl.write.Number
import jxl.write.WritableWorkbook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.formula.functions.T
import org.hildan.fxgson.FxGson
import tornadofx.*
import java.lang.Double.parseDouble
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern


class ParseUtil {
    class LocalDateTimeSerializer : JsonSerializer<LocalDateTime?> {
        override fun serialize(
            localDateTime: LocalDateTime?,
            srcType: Type?,
            context: JsonSerializationContext
        ): JsonElement? {

            return if (localDateTime == null) null
            else {
                val year = localDateTime.year
                val month = localDateTime.month
                val day = localDateTime.dayOfMonth

                val hour = localDateTime.hour
                val minute = localDateTime.minute
                val seconds = localDateTime.second
                val json =
                    "{date:{year:$year,month:$month,day:$day},time:{hour:$hour,minute:$minute,second:$seconds}}"
                JsonParser.parseString(json).asJsonObject
            }
        }
    }

    class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext
        ): LocalDateTime? {
            return if (json != null && !json.isJsonNull) {
                val date = json.asJsonObject.get("date").asJsonObject
                val time = json.asJsonObject.get("time").asJsonObject

                val year = date.get("year").asInt
                val month = date.monthIndex()
                val day = date.get("day").asInt

                val hour = time.get("hour").asInt
                val minute = time.get("minute").asInt
                val seconds = time.get("second").asInt

                LocalDateTime.of(year, month, day, hour, minute, seconds)

            } else null
        }
    }

    object ParseGson {
        var gson: Gson? = null
        private val builder = FxGson.coreBuilder()
            .apply {
                registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
                registerTypeAdapter(
                    LocalDateTime::class.java,
                    LocalDateTimeDeserializer()
                )
                disableHtmlEscaping()
            }

        init {
            gson = if (gson == null) builder.create() else gson
        }

    }

    companion object {

        val gson: Gson by lazy { ParseGson.gson!! }

        fun <T> T.toMap(): Map<String, Any> {
            return convert()
        }

        fun JsonObject.monthIndex(): Int {
            val value = this.get("month")
            return try {
                value.asInt
            } catch (e: NumberFormatException) {
                val date: Date = SimpleDateFormat("MMMM").parse(value.asString)
                val cal = Calendar.getInstance()
                cal.time = date
                cal[Calendar.MONTH] + 1
            }
        }


        inline fun <I, reified O> I.convert(): O {
            val json = this.toJson()
            return gson.fromJson(json, object : TypeToken<O>() {}.type)
        }

        inline fun <reified O> String.convert(): O? {
            return try {
                gson.fromJson(this, object : TypeToken<O>() {}.type)
            } catch (e: Exception) {
                null
            }
        }

        fun <T> String.convert(type: Type): List<T> {
            return gson.fromJson(this, type)
        }

        fun <T> String.convert(kClass: Class<T>): T {
            return gson.fromJson(this, kClass)
        }

        fun <K> K.toJson(): String {
            return gson.toJson(this)
        }

        fun User?.isAdmin(): SimpleBooleanProperty {
            return SimpleBooleanProperty(this != null && UserGroup.Admin.name == this.userGroup)
        }

        fun String?.isValidContainerNo() =
            !this.isNullOrEmpty() && Pattern.compile("^[aA-zZ]{4}\\d{7}$").matcher(this).matches()


        fun String?.capitalize(): String {
            return try {
                when {
                    this.isNullOrEmpty() -> ""
                    this.length < 2 -> this.substring(0).uppercase(Locale.getDefault())
                    else -> {
                        val lower = this.lowercase(Locale.getDefault()).split(" ")
                        lower.joinToString(" ") { "${it[0].uppercaseChar()}${it.substring(1)}" }
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }

        /***
         * Bind the [LocalDateTime] to a property
         */
        fun DateTimePicker.bindPicker(property: Property<LocalDateTime>) {
            dateTimeValue.addListener { _, _, newValue ->
                property.setValue(newValue)
            }
        }

        fun String?.isValidPlateNo(): Boolean {
            val pattern = Pattern.compile("^[nN]\\d+[a-zA-Z]+\$")
            return !this.isNullOrEmpty() && pattern.matcher(this).matches()
        }

        fun String?.isValidVehicleNo(): Boolean {
            val pattern = Pattern.compile("^[hHvVsSlL]\\d{2,}$")
            return !this.isNullOrEmpty() && pattern.matcher(this).matches()
        }

        fun String?.isValidEmail(): Boolean {
            if (this.isNullOrEmpty()) return false
            val email1 = this.replace("\\s+".toRegex(), "")
            val EMAIL_STRING = ("^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
            return email1.isNotEmpty() && Pattern.compile(EMAIL_STRING)
                .matcher(email1).matches()
        }

        fun User?.isOwnProfile(): Boolean {
            return this != null && this.id == AbstractView.Account.currentUser.get().id
        }

        fun SimpleBooleanProperty.authorized(user: User?): SimpleBooleanProperty {
            return SimpleBooleanProperty(this.get() || user.isAdmin().get())
        }

        fun Pane.clearFields() {
            Platform.runLater {
                this.children.forEach { (it as? TextField)?.textProperty()?.set(null) }
                this.children.forEach { (it as? TextArea)?.textProperty()?.set(null) }
                this.children.forEach { (it as? ComboBox<AbstractModel>)?.valueProperty()?.set(null) }
            }
        }

        /**
         * If item on the list, replace old with new else add it to the list
         */
        fun <T : AbstractModel> ObservableList<T>.updateItem(item: T?) {
            when {
                item == null -> return
                this.isEmpty() -> this.add(item)
                else -> {
                    val idx = this.indexOfFirst { it.id == item.id }
                    if (idx > -1)
                        this[idx] = item
                    else this.add(item)
                }
            }

        }

        /**
         * If item on the list, replace old with new else add it to the list
         */
        fun <T : AbstractModel> MutableList<T>.updateItem(item: T?) {
            when {
                item == null -> return
                this.isEmpty() -> this.add(item)
                else -> {
                    val idx = this.indexOfFirst { it.id == item.id }
                    if (idx > -1)
                        this[idx] = item
                    else this.add(item)
                }
            }
        }

        fun TextField.numberValidation(msg: String) =
            validator(ValidationTrigger.OnChange()) {
                if (it.isNumber())
                    null else error(msg)
            }

        /**
         * Convert a boolean value in a cell to yes if true else no string
         */
        fun <T> TableColumn<T, Boolean>.toYesNo() {
            this.setCellFactory {
                object : TableCell<T, Boolean>() {
                    override fun updateItem(item: Boolean?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty) null else if (item == true) "Yes" else "No"
                    }
                }
            }
        }


        fun TextField.toUppercase() {
            filterInput { change ->
                change.text = change.text.uppercase(Locale.getDefault())
                true
            }
        }

        fun <T> TableColumn<T, String?>.capitalize() {
            this.setCellFactory {
                object : TableCell<T, String?>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else item.capitalize()
                    }
                }
            }
        }

        /**
         * A to string method for List
         */
        fun <T, S : AbstractModel> TableColumn<T, ObservableList<S>>.listToString() {
            this.setCellFactory {
                object : TableCell<T, ObservableList<S>?>() {
                    override fun updateItem(item: ObservableList<S>?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text =
                            if (item.isNullOrEmpty())
                                null
                            else
                                item.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString(separator = "\n")

                    }
                }
            }
        }


        @JvmName("moneyFormatDouble")
        fun <T> TableColumn<T, Double>.moneyFormat(): TableColumn<T, Double> {
            this.setCellFactory {
                object : TableCell<T, Double?>() {
                    override fun updateItem(item: Double?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else String.format("NAD %.2f", item.toDouble())
                    }
                }
            }
            return this
        }

        @JvmName("moneyFormatFloat")
        fun <T> TableColumn<T, Float?>.moneyFormat(): TableColumn<T, Float?> {
            this.setCellFactory {
                object : TableCell<T, Float?>() {
                    override fun updateItem(item: Float?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else String.format("NAD %.2f", item.toDouble())
                    }
                }
            }
            return this
        }

        fun <T> TableColumn<T, Double>.toSingleDecimal(): TableColumn<T, Double> {
            this.setCellFactory {
                object : TableCell<T, Double?>() {
                    override fun updateItem(item: Double?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else String.format("%.1f", item.toDouble())
                    }
                }
            }
            return this
        }

        @JvmName("moneyFormatString")
        fun <T> TableColumn<T, String>.moneyFormat(): TableColumn<T, String> {
            this.setCellFactory {
                object : TableCell<T, String?>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else String.format("NAD %.2f", item.toDouble())
                    }
                }
            }
            return this
        }

        fun <T> TableColumn<T, String?>.toUppercase() {
            this.setCellFactory {
                object : TableCell<T, String?>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (item == null || empty)
                            null
                        else item.uppercase(Locale.getDefault())
                    }
                }
            }
        }

        fun <T : AbstractModel> TableColumn<T, LocalDateTime?>.localDateTimeFormat(): TableColumn<T, LocalDateTime?> {
            this.setCellFactory {
                object : TableCell<T, LocalDateTime?>() {
                    override fun updateItem(item: LocalDateTime?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else item._24()
                    }
                }
            }
            return this
        }

        fun <T : AbstractModel> TableColumn<T, LocalDate?>.localDateFormat() {
            this.setCellFactory {
                object : TableCell<T, LocalDate?>() {
                    override fun updateItem(item: LocalDate?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else item._24()
                    }
                }
            }
        }

        fun <T : AbstractModel> TableColumn<T, LocalDateTime?>.flagDueToday(): TableColumn<T, LocalDateTime?> {
            this.setCellFactory {
                object : TableCell<T, LocalDateTime?>() {
                    override fun updateItem(item: LocalDateTime?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else item._24()
                        style {
                            backgroundColor += Color.GREEN
//                            backgroundColor += when {
//                                item == null -> Color.WHITE
//                                item.minusHours(24).isBefore(localDateTimeToday()) ->
//                                    Color.RED
//                                item.minusHours(48).isBefore(localDateTimeToday()) -> Color.ORANGE
//                                else -> Color.YELLOW
//                            }
                        }
                    }
                }
            }
            return this
        }

        fun SimpleObjectProperty<LocalDateTime>.pickerBind(other: SimpleObjectProperty<LocalDateTime?>) {
            other.set(this.value)
            other.addListener { _, _, newValue ->
                when (newValue) {
                    null -> this.set(null)
                    else -> this.set(other.get())
                }
            }

            this.addListener { _, _, newValue ->
                when (newValue) {
                    null -> other.set(null)
                    else -> other.set(this.get())
                }
            }
        }

        fun TextArea.numberValidation(msg: String) =
            validator(ValidationTrigger.OnChange()) {
                if (it.isNumber())
                    null else error(msg)
            }

        fun TextField.generalTxtFieldValidation(msg: String, len: Int) =
            validator(ValidationTrigger.OnChange()) {
                if (!text.isNullOrEmpty() && text.toString().length >= len)
                    null else error(msg)
            }

        fun TextField.optionalTxtFieldValidation(msg: String, len: Int) =
            validator(ValidationTrigger.OnChange()) {
                if (text.isNullOrEmpty() || text.toString().length >= len)
                    null else error(msg)
            }

        fun TextField.moneyValidation() =
            validator(ValidationTrigger.OnChange()) {

                if (text.isMoney())
                    null else error("Enter a valid money value.")
            }

        fun String.isMoney() = Pattern.compile("\\d[0-9]+(\\.\\d{2})?").matcher(this).matches()


        fun TextArea.generalTxtFieldValidation(msg: String, len: Int) =
            validator(ValidationTrigger.OnChange()) {
                if (!text.isNullOrEmpty() && text.toString().length >= len)
                    null else error(msg)
            }


        fun TextField.validateEntryNo() =
            validator(ValidationTrigger.OnChange()) {
                if (text.isValidEntryNo())
                    null else error("Invalid Entry No")
            }

        fun TextField.moneyFormat(): TextField {
            filterInput { change ->
                change.text = if (change.text.isNumber())
                    String.format("%.2f", change.text)
                else ""
                true
            }
            return this
        }

        fun TextField.validateQuickBooksNo() =
            validator(ValidationTrigger.OnChange()) {
                if (text.isValidQuickBooksNo())
                    null else error("Invalid Quick Books No")
            }


        private fun String?.isValidEntryNo() = Pattern.compile("^-\\d{4}").matcher(this ?: "").matches()
        private fun String?.isValidQuickBooksNo() = Pattern.compile("^\\w{8}-\\w{10}").matcher(this ?: "").matches()

        fun Any?.isNumber(): Boolean {
            val value = this.toString()
            return value != "null" &&
                    try {
                        parseDouble(value.replace(',', '.'))
                        true
                    } catch (e: Exception) {
                        false
                    }
        }

        fun Any?.toDouble(): Double {
            return if (!this.isNumber())
                0.0
            else {
                parseDouble(this.toString().replace(',', '.'))
            }
        }

        fun Int?.isOldId() = this != null && this > 0

        fun String?.strip(): String {
            return this?.trim() ?: ""
        }

        fun String?.isEmptyStr(): Boolean {
            return (this?.replace(" ", "") ?: "").isEmpty()
        }

        fun User?.isInvalid() = this == null || this.id == null

        fun <T> ComboBox<T>.bindCombo(property: Property<T>) {
            bind(property)
            bindSelected(property)
        }

        /***
         * Export fuel transaction records to  excel for further processing
         * @param wkb the workbook to export
         * @param sheetList [ArrayList<List<T>>]  to export
         * @param sheetNameList of sheet names each corresponding to the list of model to export
         */

        suspend fun <T : AbstractModel> toExcelSpreedSheet(
            wkb: WritableWorkbook,
            sheetList: ArrayList<List<T>>,
            sheetNameList: ArrayList<String>
        ): Results {
            return try {
                withContext(Dispatchers.Default) {
                    sheetList.withIndex().forEach {
                        val sheetData = it.value //
                        if (sheetData.isNotEmpty()) {
                            wkb.createSheet(sheetNameList[it.index], it.index).apply {
                                var colIndex = 0
                                var rowIndex = 0
                                val colHeaders = sheetData[0].data().map { it.first }
                                colHeaders.forEach { addCell(Label(colIndex++, rowIndex, it)) }

                                sheetData.forEach { model ->
                                    rowIndex++
                                    colIndex = 0
                                    model.data().forEach {
                                        if (it.second.isNumber())
                                            addCell(Number(colIndex++, rowIndex, it.second.toString().toDouble()))
                                        else
                                            addCell(jxl.write.Label(colIndex++, rowIndex, it.second.toString()))
                                    }
                                }
                            }
                        }
                    }
                    Results.Success<T>(code = Results.Success.CODE.WRITE_SUCCESS)
                }
            } catch (e: Exception) {
                Results.Error(e)
            } finally {
                try {
                    wkb.write()
                    wkb.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}


