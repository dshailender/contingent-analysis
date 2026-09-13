"""Domain data models for Contingent Claims Analysis (OPM) Valuation Engine.

Provides typed Pydantic models for inputs, derived capitalization,
breakpoint schedules, claims, OPM allocations, waterfalls, holdings, and exports.
"""

from typing import List, Dict, Optional, Any, Literal
from pydantic import BaseModel, Field


class SecurityInput(BaseModel):
    """Raw input row representing an instrument in the capitalization ledger."""
    security: str = Field(..., description="Security or share class name, e.g. Series A, Common Stock")
    security_subtype: Literal["Preferred Stock", "Common Stock", "Option", "Warrant", "Other"] = Field(
        "Preferred Stock", description="Instrument category"
    )
    shares: float = Field(0.0, description="Number of shares or instruments outstanding")
    exercise_price: Optional[float] = Field(None, description="Weighted average exercise price (Options/Warrants)")
    original_issue_price: Optional[float] = Field(None, description="Original issue price per share (Preferred)")
    conversion_price: Optional[float] = Field(None, description="Conversion price per share (Preferred)")
    liquidation_multiplier: Optional[float] = Field(1.0, description="Multiple applied to OIP for liquidation preference")
    participation: Literal["Yes", "No", "NA"] = Field("NA", description="Participation feature for Preferred")
    max_participation_cap: Optional[str] = Field("NA", description="Participation cap string, e.g. 'No Cap', '2x', '3x'")
    seniority: Optional[int] = Field(None, description="Seniority rank integer (1 = highest priority)")
    issue_date: Optional[str] = Field(None, description="Issuance date for dividend accrual (YYYY-MM-DD)")
    dividend_rate: Optional[float] = Field(None, description="Annual dividend rate as percentage, e.g. 8.0 for 8%")
    compounding_convention: Optional[Literal["Simple Interest", "Annual", "Semi-Annual", "Quarterly", "Daily"]] = Field(
        "Annual", description="Dividend compounding interval"
    )
    dividends_paid_to_date: Optional[float] = Field(0.0, description="Cumulative dividends satisfied to date per share")


class DerivedSecurity(BaseModel):
    """Derived financial terms for a security class."""
    security: str
    security_subtype: str
    shares: float
    original_issue_price: float
    conversion_price: Optional[float]
    conversion_ratio: float
    liquidation_multiplier: float
    seniority: int
    participation: str
    cap_mult: Optional[float] = None
    per_share_dividend: float = 0.0
    total_accrued_dividends: float = 0.0
    liquidation_preference_per_share: float = 0.0
    total_liquidation_preference: float = 0.0
    fully_diluted_shares: float = 0.0
    exercise_price: float = 0.0
    total_exercise_proceeds: float = 0.0


class BreakpointTier(BaseModel):
    """Single tier interval in the cumulative equity breakpoint schedule."""
    tier: int
    start_equity: float
    end_equity: float
    width: float
    claimants_description: str
    event_description: str
    is_thereafter: bool = False


class ClaimTierAllocation(BaseModel):
    """Claimant sharing percentages and dollar allocations for a breakpoint tier."""
    tier: int
    from_equity: float
    to_equity: float
    width: float
    is_thereafter: bool = False
    sharing_percentages: Dict[str, float] = Field(default_factory=dict)
    dollar_claims: Dict[str, float] = Field(default_factory=dict)


class OpmTranche(BaseModel):
    """Incremental Black-Scholes call option tranche across adjacent breakpoint strikes."""
    tier: int
    strike_low: float
    strike_high: float
    call_low: float
    call_high: float
    incremental_call: float


class OpmAllocationResult(BaseModel):
    """Results of Option Pricing Method allocation."""
    equity_value: float
    term: float
    risk_free_rate_effective: float
    risk_free_rate_continuous: float
    volatility: float
    dividend_yield: float
    tranches: List[OpmTranche] = Field(default_factory=list)
    allocated_values: Dict[str, float] = Field(default_factory=dict)
    per_share_values: Dict[str, float] = Field(default_factory=dict)
    percent_allocations: Dict[str, float] = Field(default_factory=dict)
    total_allocated: float = 0.0


class WaterfallAllocationItem(BaseModel):
    """Proceeds distributed to a security class in a scenario waterfall."""
    security: str
    shares: float
    proceeds: float
    proceeds_per_share: float
    percent_recovery: float
    percent_of_total: float


class WaterfallResult(BaseModel):
    """Overall waterfall distribution results at a specified equity value."""
    applied_equity: float
    distribution: List[WaterfallAllocationItem] = Field(default_factory=list)
    total_proceeds: float = 0.0


class ComparativeWaterfallItem(BaseModel):
    """Side-by-side comparative distribution row between Calibration and Valuation structures."""
    security: str
    cal_shares: Optional[float] = None
    cal_fd_ownership: Optional[float] = None
    val_shares: Optional[float] = None
    val_fd_ownership: Optional[float] = None
    cal_distribution: Optional[float] = None
    cal_per_share: Optional[float] = None
    val_distribution: Optional[float] = None
    val_per_share: Optional[float] = None
    change_in_distribution: Optional[float] = None


class HoldingInput(BaseModel):
    """Client vehicle holding position."""
    fund: str = Field(..., description="Fund or vehicle name")
    security: str = Field(..., description="Target security class name")
    units: float = Field(..., description="Units or shares held")
    cost: float = Field(0.0, description="Cost basis or invested amount")


class HoldingResultItem(BaseModel):
    """Evaluated position for a client holding."""
    fund: str
    security: str
    units: float
    cost: float
    fair_value_per_share: float
    concluded_fair_value: float
    class_ownership_pct: float
    fully_diluted_ownership_pct: float
    moic: Optional[float] = None


class FundSubtotal(BaseModel):
    """Subtotal metrics per investment fund or vehicle."""
    fund: str
    total_units: float
    total_cost: float
    total_value: float
    moic: Optional[float] = None


class HoldingsSummary(BaseModel):
    """Portfolio-wide multi-fund summary."""
    items: List[HoldingResultItem] = Field(default_factory=list)
    fund_subtotals: List[FundSubtotal] = Field(default_factory=list)
    total_cost: float = 0.0
    total_value: float = 0.0
    consolidated_moic: Optional[float] = None


class VolatilityStats(BaseModel):
    """Summary statistics for guideline public company volatility."""
    min: float
    q1: float
    median: float
    mean: float
    q3: float
    max: float


class ComparableCompanyVol(BaseModel):
    """Individual guideline public company market data and volatility metrics."""
    ciq_id: str
    company_name: str
    share_price: float
    market_cap: float
    total_debt: float
    preferred_equity: float
    minority_interest: float
    equity_vol: float
    asset_vol: float
    relevered_vol: Optional[float] = None
    currency: str = "USD"
    include: bool = True


class VolatilityAnalysisResult(BaseModel):
    """Complete volatility analysis output for an exhibit."""
    companies: List[ComparableCompanyVol] = Field(default_factory=list)
    equity_stats: Optional[VolatilityStats] = None
    asset_stats: Optional[VolatilityStats] = None
    relevered_stats: Optional[VolatilityStats] = None
    selected_basis: str = "equity"
    selected_stat: str = "median"
    selected_volatility: float = 0.35
    subject_equity_value: float = 0.0
    subject_debt_and_pref: float = 0.0
    subject_debt_to_equity: float = 0.0
    subject_relevered_vol: float = 0.35


class YieldCurvePoint(BaseModel):
    """Single point on an official risk-free yield curve."""
    tenor_name: str
    tenor_years: float
    rate_percent: float


class RiskFreeRateAnalysis(BaseModel):
    """Risk-free rate sourcing and interpolation details."""
    source_name: str
    as_of_date: str
    term_years: float
    interpolated_annual_effective_rate: float
    continuous_rate: float
    curve_points: List[YieldCurvePoint] = Field(default_factory=list)
    interpolation_metadata: str = ""


class ValuationRequest(BaseModel):
    """Top-level calculation request payload."""
    company_name: str = "TADO"
    client_name: str = "S2G Investments"
    report_status: str = "DRAFT - For Discussion Purposes Only"
    report_purpose: str = "Valuation Analysis"
    report_purpose_manual: Optional[str] = ""
    calibration_date: str = "2025-02-26"
    valuation_date: str = "2026-06-30"
    exit_date: str = "2027-06-30"
    day_count_basis: int = 1  # 0: US 30/360, 1: Actual/Actual, 2: Actual/360, 3: Actual/365, 4: European 30/360
    report_currency: str = "EUR"
    display_units: Literal["actual", "thousands", "millions"] = "actual"
    
    # Capital Structures
    calibration_securities: List[SecurityInput] = Field(default_factory=list)
    valuation_securities: List[SecurityInput] = Field(default_factory=list)
    
    # OPM Parameters
    calibration_security_name: str = "Series I"
    transaction_price: float = 2021.90
    rf_calibration: float = 2.1  # Annual effective %
    vol_calibration: float = 35.0  # %
    dividend_yield_calibration: float = 0.0  # %
    
    rf_valuation: float = 2.4  # Annual effective %
    vol_valuation: float = 35.0  # %
    dividend_yield_valuation: float = 0.0  # %
    
    market_adjustment: float = 0.0  # %
    company_adjustment: float = 0.0  # %
    
    waterfall_equity_source: Literal["concluded", "manual"] = "concluded"
    manual_waterfall_equity: float = 229900000.0
    
    # Client Holdings
    holdings: List[HoldingInput] = Field(default_factory=list)

    # Custom Branding & Currency
    firm_logo_base64: Optional[str] = None
    show_secondary_currency: bool = False
    secondary_currency: str = "USD"
    secondary_fx_rate: float = 1.0


class ValuationResponse(BaseModel):
    """Top-level calculation response payload."""
    company_name: str
    client_name: str
    report_status: str
    report_purpose: str
    report_currency: str
    display_units: str
    display_scale: float

    # Custom Branding & Currency
    firm_logo_base64: Optional[str] = None
    show_secondary_currency: bool = False
    secondary_currency: str = "USD"
    secondary_fx_rate: float = 1.0
    
    # Dates & Rates
    calibration_date: str
    valuation_date: str
    exit_date: str
    day_count_basis: int
    day_count_name: str
    
    term_calibration: float
    term_valuation: float
    rf_calibration_effective: float
    rf_calibration_continuous: float
    rf_valuation_effective: float
    rf_valuation_continuous: float
    
    # Calibration Analysis
    calibration_derived_securities: List[DerivedSecurity]
    calibration_breakpoints: List[BreakpointTier]
    calibration_claims: List[ClaimTierAllocation]
    calibration_solved_equity: float
    calibration_opm: OpmAllocationResult
    calibration_rf_analysis: Optional[RiskFreeRateAnalysis] = None
    
    # Valuation Analysis
    valuation_derived_securities: List[DerivedSecurity]
    valuation_breakpoints: List[BreakpointTier]
    valuation_claims: List[ClaimTierAllocation]
    concluded_equity_value: float
    valuation_opm: OpmAllocationResult
    valuation_rf_analysis: Optional[RiskFreeRateAnalysis] = None
    
    # Comparative Waterfall
    waterfall: WaterfallResult
    calibration_waterfall: Optional[WaterfallResult] = None
    comparative_waterfall: List[ComparativeWaterfallItem] = Field(default_factory=list)
    
    # Client Holdings
    holdings: HoldingsSummary
    
    # Volatility Analysis (optional / GPC bridge)
    calibration_volatility: Optional[VolatilityAnalysisResult] = None
    valuation_volatility: Optional[VolatilityAnalysisResult] = None

