package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PresentedOfferingContext
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Only [Package.identifier]/[Package.packageType] matter here; the rest is never read. */
private class FakePackage(
    override val identifier: String,
    override val packageType: PackageType,
) : Package {
    override val storeProduct: StoreProduct
        get() = throw UnsupportedOperationException("not exercised")

    override val presentedOfferingContext: PresentedOfferingContext
        get() = throw UnsupportedOperationException("not exercised")

    override val webCheckoutUrl: String? = null
}

private class FakeOffering(
    override val availablePackages: List<Package>,
    override val annual: Package?,
) : Offering {
    override val identifier: String = "default"
    override val serverDescription: String = "The standard set of packages"
    override val metadata: Map<String, Any> = emptyMap()
    override val lifetime: Package? = null
    override val sixMonth: Package? = null
    override val threeMonth: Package? = null
    override val twoMonth: Package? = null
    override val monthly: Package? = null
    override val weekly: Package? = null
    override val webCheckoutUrl: String? = null
}

class YearlyPackageSelectionTest {
    /**
     * Mirrors the live RevenueCat dashboard exactly: the `default` offering holds a single package
     * whose identifier is the predefined `$rc_annual`. "yearly" appears only as the identifier of a
     * *product inside* that package, never as a package identifier.
     */
    private fun liveDashboardShapedOfferings(): Pair<Offerings, Package> {
        val annual = FakePackage(identifier = "\$rc_annual", packageType = PackageType.ANNUAL)
        val offering = FakeOffering(availablePackages = listOf(annual), annual = annual)
        return Offerings(all = mapOf(offering.identifier to offering), current = offering) to annual
    }

    @Test
    fun resolvesTheAnnualPackageConfiguredInTheDashboard() {
        val (offerings, annual) = liveDashboardShapedOfferings()

        val resolution = resolveYearlyPackageFrom(offerings)

        assertIs<YearlyPackageResolution.Resolved>(resolution)
        assertSame(annual, resolution.yearlyPackage, "must resolve the dashboard's \$rc_annual package")
    }

    @Test
    fun literalYearlyIdentifierNeverMatchesAPackage() {
        // Regression guard: the pre-fix code called getPackage("yearly"), which can never match
        // because "yearly" identifies a product inside the package, not the package. That made
        // every real purchase fail with "package unavailable" while the paywall still showed a
        // hardcoded price, so the paywall looked healthy against a product that could not be sold.
        val (offerings, _) = liveDashboardShapedOfferings()

        assertNull(offerings.current?.getPackage("yearly"))
        assertIs<YearlyPackageResolution.Resolved>(resolveYearlyPackageFrom(offerings))
    }

    @Test
    fun missingCurrentOfferingIsReportedDistinctlyFromAMissingAnnualPackage() {
        // These two need opposite fixes in the dashboard — mark an offering Default, versus attach
        // a product to the annual slot of the offering that already is. Collapsing them into one
        // opaque error is what made the original failure cost a full debugging session, so the
        // distinction is the behaviour under test, not merely the message text.
        val noCurrent = resolveYearlyPackageFrom(Offerings(all = emptyMap(), current = null))
        val emptyAnnualSlot =
            FakeOffering(availablePackages = emptyList(), annual = null).let { offering ->
                resolveYearlyPackageFrom(Offerings(all = mapOf(offering.identifier to offering), current = offering))
            }

        assertIs<YearlyPackageResolution.NoCurrentOffering>(noCurrent)
        assertIs<YearlyPackageResolution.NoAnnualPackage>(emptyAnnualSlot)
        assertEquals(
            2,
            setOf(noCurrent.message, emptyAnnualSlot.message).size,
            "each failure mode must name its own cause, not share one generic message",
        )
    }

    @Test
    fun offeringsFetchFailureCarriesTheUnderlyingCause() {
        // A failed getOfferings call (network down, bad credentials) must not be indistinguishable
        // from a misconfigured dashboard: the SDK's own message is what tells them apart.
        val failure = YearlyPackageResolution.OfferingsUnavailable("The internet connection appears to be offline.")

        assertEquals("The internet connection appears to be offline.", failure.message)
    }
}
