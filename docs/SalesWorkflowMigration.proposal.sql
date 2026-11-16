-- ARCHIVED PROPOSAL 2026-11-16: DO NOT EXECUTE. Owner requires unchanged schema.
-- Two-party contract approval is cancelled; ContractRevisions/ContractDecisions are not prerequisites for OTP signing.
-- REVIEW ONLY. Run in a disposable SQL Server copy after approval.
-- This file adds storage only. It does not activate chat, approval, or reconciliation.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

CREATE TABLE dbo.SalesConversations (
    ConversationID BIGINT IDENTITY PRIMARY KEY,
    ConsultationID INT NOT NULL UNIQUE REFERENCES dbo.ConsultationRequests(ConsultationRequestID),
    CustomerID INT NOT NULL REFERENCES dbo.Customers(CustomerID),
    SalesEmployeeID INT NOT NULL REFERENCES dbo.Employees(EmployeeID),
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    ClosedAt DATETIME2(3) NULL
);

CREATE TABLE dbo.SalesProposals (
    ProposalID BIGINT IDENTITY PRIMARY KEY,
    ConversationID BIGINT NOT NULL REFERENCES dbo.SalesConversations(ConversationID),
    OrderID INT NOT NULL UNIQUE REFERENCES dbo.Orders(OrderID),
    DepositMethodID INT NOT NULL REFERENCES dbo.PaymentMethods(PaymentMethodID),
    Status VARCHAR(20) NOT NULL DEFAULT 'PROPOSED'
        CHECK (Status IN ('PROPOSED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    DecidedByUserID INT NULL REFERENCES dbo.Users(UserID),
    DecidedAt DATETIME2(3) NULL,
    DecisionReason NVARCHAR(500) NULL,
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.SalesMessages (
    MessageID BIGINT IDENTITY PRIMARY KEY,
    ConversationID BIGINT NOT NULL REFERENCES dbo.SalesConversations(ConversationID),
    AuthorUserID INT NULL REFERENCES dbo.Users(UserID),
    Body NVARCHAR(2000) NULL,
    EventKey VARCHAR(150) NULL,
    ProposalID BIGINT NULL REFERENCES dbo.SalesProposals(ProposalID),
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK ((AuthorUserID IS NOT NULL AND Body IS NOT NULL AND EventKey IS NULL)
        OR (AuthorUserID IS NULL AND EventKey IS NOT NULL))
);
CREATE UNIQUE INDEX UX_SalesMessages_Event
    ON dbo.SalesMessages(EventKey) WHERE EventKey IS NOT NULL;
CREATE INDEX IX_SalesMessages_Conversation
    ON dbo.SalesMessages(ConversationID, MessageID);

CREATE TABLE dbo.ContractRevisions (
    RevisionID BIGINT IDENTITY PRIMARY KEY,
    ContractID INT NOT NULL REFERENCES dbo.Contracts(ContractID),
    RevisionNumber INT NOT NULL CHECK (RevisionNumber > 0),
    IsCurrent BIT NOT NULL,
    DocumentSha256 BINARY(32) NOT NULL,
    DraftDocx VARBINARY(MAX) NOT NULL,
    CreatedByUserID INT NOT NULL REFERENCES dbo.Users(UserID),
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    SignedAt DATETIME2(3) NULL,
    SigningOtpID INT NULL REFERENCES dbo.OTPVerifications(OTPVerificationID),
    UNIQUE (ContractID, RevisionNumber),
    CHECK ((SignedAt IS NULL AND SigningOtpID IS NULL)
        OR (SignedAt IS NOT NULL AND SigningOtpID IS NOT NULL))
);
CREATE UNIQUE INDEX UX_ContractRevisions_Current ON dbo.ContractRevisions(ContractID) WHERE IsCurrent=1;
CREATE UNIQUE INDEX UX_ContractRevisions_Otp ON dbo.ContractRevisions(SigningOtpID) WHERE SigningOtpID IS NOT NULL;

CREATE TABLE dbo.ContractDecisions (
    RevisionID BIGINT NOT NULL REFERENCES dbo.ContractRevisions(RevisionID),
    ReviewerRole VARCHAR(16) NOT NULL CHECK (ReviewerRole IN ('ACCOUNTANT', 'CUSTOMER')),
    ReviewerUserID INT NOT NULL REFERENCES dbo.Users(UserID),
    DocumentSha256 BINARY(32) NOT NULL,
    Decision VARCHAR(20) NOT NULL CHECK (Decision IN ('APPROVED', 'REJECTED', 'CHANGES_REQUESTED')),
    Reason NVARCHAR(500) NULL,
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    PRIMARY KEY (RevisionID, ReviewerRole),
    CHECK (Decision='APPROVED' OR NULLIF(LTRIM(RTRIM(Reason)), N'') IS NOT NULL)
);

CREATE TABLE dbo.SePayEvents (
    EventID BIGINT PRIMARY KEY,
    PaymentID INT NULL REFERENCES dbo.Payments(PaymentID),
    Gateway VARCHAR(40) NOT NULL,
    AccountNumber VARCHAR(50) NOT NULL,
    SubAccount VARCHAR(50) NOT NULL,
    BankReference VARCHAR(80) NULL,
    PaymentReference VARCHAR(80) NULL,
    Amount DECIMAL(18,2) NOT NULL,
    BankTransferAt DATETIME2(3) NULL,
    ReceivedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    Content NVARCHAR(500) NULL,
    Outcome VARCHAR(20) NOT NULL CHECK (Outcome IN ('MATCHED','REQUIRES_REVIEW','IGNORED')),
    FailureReason NVARCHAR(240) NULL,
    CHECK (Outcome<>'MATCHED' OR (PaymentID IS NOT NULL AND BankReference IS NOT NULL AND Amount>0))
);
CREATE UNIQUE INDEX UX_SePayEvents_AppliedBankReference
    ON dbo.SePayEvents(Gateway, AccountNumber, BankReference) WHERE Outcome='MATCHED';
CREATE UNIQUE INDEX UX_SePayEvents_AppliedPayment
    ON dbo.SePayEvents(PaymentID) WHERE Outcome='MATCHED';

CREATE TABLE dbo.PaymentReconciliations (
    PaymentID INT PRIMARY KEY REFERENCES dbo.Payments(PaymentID),
    EmployeeID INT NOT NULL REFERENCES dbo.Employees(EmployeeID),
    StatementReference VARCHAR(80) NOT NULL,
    Note NVARCHAR(500) NULL,
    ReconciledAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.SalesDocuments (
    DocumentID BIGINT IDENTITY PRIMARY KEY,
    OrderID INT NOT NULL REFERENCES dbo.Orders(OrderID),
    CustomerID INT NOT NULL REFERENCES dbo.Customers(CustomerID),
    Kind VARCHAR(24) NOT NULL CHECK (Kind IN ('DEPOSIT_RECEIPT','SIGNED_CONTRACT','ISSUED_INVOICE')),
    PaymentID INT NULL REFERENCES dbo.Payments(PaymentID),
    RevisionID BIGINT NULL REFERENCES dbo.ContractRevisions(RevisionID),
    InvoiceID INT NULL REFERENCES dbo.Invoices(InvoiceID),
    TemplateSha256 BINARY(32) NOT NULL,
    DocumentSha256 BINARY(32) NOT NULL,
    Docx VARBINARY(MAX) NOT NULL,
    CreatedAt DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (OrderID, Kind),
    CHECK ((Kind='DEPOSIT_RECEIPT' AND PaymentID IS NOT NULL AND RevisionID IS NULL AND InvoiceID IS NULL)
        OR (Kind='SIGNED_CONTRACT' AND PaymentID IS NULL AND RevisionID IS NOT NULL AND InvoiceID IS NULL)
        OR (Kind='ISSUED_INVOICE' AND PaymentID IS NULL AND RevisionID IS NULL AND InvoiceID IS NOT NULL))
);

COMMIT TRANSACTION;
