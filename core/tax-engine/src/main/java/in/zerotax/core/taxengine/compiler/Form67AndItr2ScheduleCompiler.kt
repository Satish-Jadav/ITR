package `in`.zerotax.core.taxengine.compiler

import org.json.JSONArray
import org.json.JSONObject

data class ForeignIncomeScheduleEntry(
    val countryCode: String,
    val countryName: String,
    val taxpayerIdentificationNumber: String,
    val headOfIncome: String, // "OS", "CG", "SALARY"
    val grossIncomeInr: Long,
    val foreignTaxPaidInr: Long,
    val taxReliefClaimedSec90_91: Long,
    val dtaaArticleNumber: String = "Article 10 / 23"
)

object Form67AndItr2ScheduleCompiler {

    fun compileForm67AndScheduleTrJson(
        pan: String,
        assessmentYear: String,
        entries: List<ForeignIncomeScheduleEntry>
    ): String {
        val root = JSONObject()
        val form67 = JSONObject()
        form67.put("FormName", "Form 67")
        form67.put("PAN", pan)
        form67.put("AssessmentYear", assessmentYear)

        val scheduleFsi = JSONArray()
        val scheduleTr = JSONArray()
        var totalTaxRelief = 0L

        for (entry in entries) {
            val fsiItem = JSONObject()
            fsiItem.put("CountryCode", entry.countryCode)
            fsiItem.put("CountryName", entry.countryName)
            fsiItem.put("TIN", entry.taxpayerIdentificationNumber)
            fsiItem.put("HeadOfIncome", entry.headOfIncome)
            fsiItem.put("IncomeOutsideIndia", entry.grossIncomeInr)
            fsiItem.put("TaxPaidOutsideIndia", entry.foreignTaxPaidInr)
            fsiItem.put("TaxReliefAvailable", entry.taxReliefClaimedSec90_91)
            scheduleFsi.put(fsiItem)

            val trItem = JSONObject()
            trItem.put("CountryCode", entry.countryCode)
            trItem.put("TotalTaxesPaidOutsideIndia", entry.foreignTaxPaidInr)
            trItem.put("TotalTaxReliefClaimed", entry.taxReliefClaimedSec90_91)
            trItem.put("DtaaArticle", entry.dtaaArticleNumber)
            scheduleTr.put(trItem)

            totalTaxRelief += entry.taxReliefClaimedSec90_91
        }

        form67.put("ScheduleFSI", scheduleFsi)
        form67.put("ScheduleTR", scheduleTr)
        form67.put("TotalReliefClaimedUnderSection90_91", totalTaxRelief)
        root.put("Form67Verification", form67)

        return root.toString(2)
    }
}

