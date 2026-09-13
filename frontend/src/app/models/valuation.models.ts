/**
 * TypeScript data models matching FastAPI / Pydantic schemas.
 */

export interface SecurityInput {
  security: string;
  security_subtype: string;
  shares: number;
  exercise_price: number | null;
  original_issue_price: number | null;
  conversion_price: number | null;
  liquidation_multiplier: number;
  participation: string;
  max_participation_cap: string;
  seniority: number | null;
  issue_date: string | null;
  dividend_rate: number | null;
  compounding_convention: string;
  dividends_paid_to_date: number;
}

export interface DerivedSecurity {
  security: string;
  security_subtype: string;
  shares: number;
  original_issue_price: number;
  conversion_price: number;
  conversion_ratio: number;
  liquidation_multiplier: number;
  participation: string;
  max_participation_cap: string;
  seniority: number;
  issue_date: string;
  annual_dividend_rate: number;
  compounding_convention: string;
  accrual_years: number;
  accrued_dividends_per_share: number;
  dividends_paid_to_date: number;
  total_liquidation_preference: number;
  liquidation_preference_per_share: number;
  conversion_threshold_equity: number;
  participation_cap_equity: number;
  fully_diluted_shares: number;
  exercise_price: number;
  is_preferred: boolean;
  is_option_warrant: boolean;
  is_participating: boolean;
}

export interface BreakpointTier {
  tier: number;
  start_equity: number;
  end_equity: number;
  width?: number;
  incremental_equity?: number;
  claimants_description?: string;
  event_description?: string;
  is_thereafter?: boolean;
  active_securities?: string[];
  participating_securities?: string[];
  events_description?: string;
}

export interface ClaimTierAllocation {
  tier: number;
  from_equity?: number;
  to_equity?: number;
  start_equity?: number;
  end_equity?: number;
  width?: number;
  is_thereafter?: boolean;
  dollar_claims: Record<string, number>;
  sharing_percentages?: Record<string, number>;
  percent_claims?: Record<string, number>;
  total_tier_claim?: number;
}

export interface OpmTranche {
  tier: number;
  strike_low: number;
  strike_high: number;
  call_low: number;
  call_high: number;
  incremental_call: number;
}

export interface OpmAllocationResult {
  equity_value: number;
  term: number;
  risk_free_rate_effective: number;
  risk_free_rate_continuous: number;
  volatility: number;
  dividend_yield: number;
  tranches: OpmTranche[];
  allocated_values: Record<string, number>;
  per_share_values: Record<string, number>;
  percent_allocations: Record<string, number>;
  total_allocated: number;
}

export interface HoldingInput {
  fund: string;
  security: string;
  units: number;
  cost: number;
}

export interface HoldingResultItem {
  fund: string;
  security: string;
  units: number;
  cost: number;
  fair_value_per_share: number;
  concluded_fair_value: number;
  class_ownership_pct: number;
  fully_diluted_ownership_pct: number;
  moic: number | null;
}

export interface FundSubtotal {
  fund: string;
  total_units: number;
  total_cost: number;
  total_value: number;
  moic: number | null;
}

export interface HoldingsSummary {
  items: HoldingResultItem[];
  fund_subtotals: FundSubtotal[];
  total_cost: number;
  total_value: number;
  consolidated_moic: number | null;
}

export interface WaterfallDistribution {
  security: string;
  shares: number;
  proceeds?: number;
  proceeds_per_share?: number;
  percent_recovery?: number;
  percent_of_total?: number;
  liquidation_preference?: number;
  preferred_recovery?: number;
  residual_common_recovery?: number;
  total_recovery?: number;
  per_share_distribution?: number;
  percent_of_equity?: number;
}

export interface WaterfallAllocationItem extends WaterfallDistribution {}

export interface WaterfallResult {
  applied_equity?: number;
  equity_value?: number;
  distribution?: WaterfallAllocationItem[];
  distributions?: WaterfallAllocationItem[];
  total_proceeds: number;
}

export interface ComparativeWaterfallItem {
  security: string;
  cal_shares?: number;
  cal_fd_ownership?: number;
  val_shares?: number;
  val_fd_ownership?: number;
  cal_distribution?: number;
  cal_per_share?: number;
  val_distribution?: number;
  val_per_share?: number;
  change_in_distribution?: number;
}

export interface YieldCurvePoint {
  tenor_name: string;
  tenor_years: number;
  rate_percent: number;
}

export interface RiskFreeRateAnalysis {
  source_name: string;
  as_of_date: string;
  term_years: number;
  interpolated_annual_effective_rate: number;
  continuous_rate: number;
  curve_points: YieldCurvePoint[];
  interpolation_metadata: string;
}

export interface ComparableCompanyVol {
  ticker: string;
  company_name: string;
  market_cap: number;
  total_debt: number;
  cash: number;
  equity_volatility: number;
  debt_to_equity: number;
  merton_asset_volatility: number;
}

export interface VolatilityStats {
  mean: number;
  median: number;
  min: number;
  max: number;
  count: number;
}

export interface VolatilityAnalysisResult {
  as_of_date: string;
  peers: ComparableCompanyVol[];
  raw_equity_volatility_stats: VolatilityStats;
  merton_asset_volatility_stats: VolatilityStats;
  selected_asset_volatility: number;
  concluded_relevered_equity_volatility: number;
}

export interface ValuationRequest {
  company_name: string;
  client_name: string;
  report_status: string;
  report_purpose: string;
  report_purpose_manual?: string;
  calibration_date: string;
  valuation_date: string;
  exit_date: string;
  day_count_basis: number;
  report_currency: string;
  display_units: 'actual' | 'thousands' | 'millions';
  firm_logo_base64?: string;
  show_secondary_currency?: boolean;
  secondary_currency?: string;
  secondary_fx_rate?: number;
  calibration_securities: SecurityInput[];
  valuation_securities: SecurityInput[];
  calibration_security_name: string;
  transaction_price: number;
  rf_calibration: number;
  vol_calibration: number;
  dividend_yield_calibration: number;
  rf_valuation: number;
  vol_valuation: number;
  dividend_yield_valuation: number;
  market_adjustment: number;
  company_adjustment: number;
  waterfall_equity_source: 'concluded' | 'manual';
  manual_waterfall_equity: number;
  holdings: HoldingInput[];
}

export interface ValuationResponse {
  company_name: string;
  client_name: string;
  report_status: string;
  report_purpose: string;
  report_currency: string;
  display_units: string;
  display_scale: number;
  firm_logo_base64?: string;
  show_secondary_currency?: boolean;
  secondary_currency?: string;
  secondary_fx_rate?: number;
  calibration_date: string;
  valuation_date: string;
  exit_date: string;
  day_count_basis: number;
  day_count_name: string;
  term_calibration: number;
  term_valuation: number;
  rf_calibration_effective: number;
  rf_calibration_continuous: number;
  rf_valuation_effective: number;
  rf_valuation_continuous: number;
  calibration_derived_securities: DerivedSecurity[];
  calibration_breakpoints: BreakpointTier[];
  calibration_claims: ClaimTierAllocation[];
  calibration_solved_equity: number;
  calibration_opm: OpmAllocationResult;
  calibration_rf_analysis?: RiskFreeRateAnalysis;
  valuation_derived_securities: DerivedSecurity[];
  valuation_breakpoints: BreakpointTier[];
  valuation_claims: ClaimTierAllocation[];
  concluded_equity_value: number;
  valuation_opm: OpmAllocationResult;
  valuation_rf_analysis?: RiskFreeRateAnalysis;
  waterfall: WaterfallResult;
  calibration_waterfall?: WaterfallResult;
  comparative_waterfall?: ComparativeWaterfallItem[];
  holdings: HoldingsSummary;
  calibration_volatility?: VolatilityAnalysisResult;
  valuation_volatility?: VolatilityAnalysisResult;
}

export interface ValidationResult {
  valid: boolean;
  issues: string[];
}

