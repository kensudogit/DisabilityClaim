export type BeneficiaryCategory = "ADULT" | "CHILD";
export type BeneficiaryStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";

export type Beneficiary = {
  id: string;
  anonymizedCode: string;
  category: BeneficiaryCategory;
  municipalityCode: string;
  municipalityName?: string;
  recipientNumber?: string;
  status: BeneficiaryStatus;
  startDate?: string;
  endDate?: string;
  staffName?: string;
};

export type RecipientCertificate = {
  id: string;
  certificateNumber: string;
  municipalityCode: string;
  municipalityName?: string;
  validFrom: string;
  validTo: string;
  serviceCategory?: string;
  monitoringPeriodMonths?: number;
};

export type ImportValidationError = {
  row: number;
  column: string;
  reason: string;
};

export type ImportJobResult = {
  jobId: string;
  totalRows: number;
  successRows: number;
  errorRows: number;
  errors: ImportValidationError[];
};

export type BillingCandidate = {
  beneficiaryId: string;
  anonymizedCode: string;
  category: BeneficiaryCategory;
  municipalityCode: string;
  municipalityName?: string;
  certificateId?: string;
  activityCount?: number;
  alreadyBilled?: boolean;
};

export type BillingBatchStatus =
  | "DRAFT"
  | "CALCULATED"
  | "VALIDATED"
  | "CONFIRMED"
  | "EXPORTED";

export type BillingBatch = {
  id: string;
  billingMonth: string;
  status: BillingBatchStatus;
  caseCount?: number;
  createdAt?: string;
};

export type CalculationTraceStep = {
  step: string;
  detail: string;
  value?: string | number | null;
};

export type BillingCase = {
  id: string;
  beneficiaryId: string;
  anonymizedCode: string;
  category: BeneficiaryCategory;
  municipalityCode: string;
  municipalityName?: string;
  baseServiceCode?: string | null;
  baseUnits?: number | null;
  additionUnits?: number | null;
  deductionUnits?: number | null;
  totalUnits?: number | null;
  unitPrice?: number | null;
  billedAmount?: number | null;
  hasError?: boolean;
  hasWarning?: boolean;
  confirmed?: boolean;
  calculationTrace?: CalculationTraceStep[];
};

export type BillingBatchDetail = BillingBatch & {
  cases: BillingCase[];
};

export type LoginResponse = {
  accessToken: string;
  tokenType?: string;
};
