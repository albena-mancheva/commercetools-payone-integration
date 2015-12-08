# Data Mapping between PAYONE and the commercetools platform

> for better readability you might want to use a ["wide github"](https://chrome.google.com/webstore/detail/wide-github/kaalofacklcidaampbokdplbklpeldpj) plugin in your Browser

TODOs:
 * go through remaining open questions with PAYONE and CT product managers
 * go through PAYONE status notification fields and responses (currently only the requests have been documented here) 

## Payment methods covered by this specification

See also: [CT Method field convention](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md)
 
| CT `method` field value |  PAYONE `clearingtype` | Additional PAYONE parameter | common name |
|---|---|---|---|
| `DIRECT_DEBIT_SEPA` | `elv` |  | Lastschrift / Direct Debit |
| `CREDIT_CARD` | `cc` | (card network and specific card data are trasnferred on the client API only -> PCI DSS !) | Credit Card |
| `BANK_TRANSFER-SOFORTUEBERWEISUNG` | `sb` | `onlinebanktransfertype=PNT` |  Sofortbanking / Sofortüberweisung (DE) |
| `BANK_TRANSFER-GIROPAY` | `sb` | `onlinebanktransfertype=GPY` |  Giropay (DE) |
| `BANK_TRANSFER-EPS` | `sb` | `onlinebanktransfertype=EPS` | eps / STUZZA (AT)  |
| `BANK_TRANSFER-POSTFINANCE_EFINANCE` | `sb` | `onlinebanktransfertype=PFF` | PostFinance e-Finance (CH)  |
| `BANK_TRANSFER-POSTFINANCE_CARD` | `sb` | `onlinebanktransfertype=PFC` | PostFinance Card (CH) |
| `BANK_TRANSFER-IDEAL` | `sb` | `onlinebanktransfertype=IDL` | iDEAL (NL) |
| `CASH_ADVANCE` | `vor` |  | Prepayment (PAYONE has access to the merchant's account to see if the money has arrived) |
| `INVOICE-DIRECT` | `rec` |  | Direct Invoice (PAYONE has access to the merchant's account to see if the money has arrived) |
| `CASH_ON_DELIVERY` | `cod` | `shippingprovider` needs to be set, see below in the field mappings | Cash on Delivery |
| `WALLET-PAYPAL` | `wlt` | `wallettype=PPE` | PayPal |
| `INSTALLMENT-KLARNA` | `fnc` | `financingtype=KLS` |  Consumer Credit / Installment via Klarna |
| `INVOICE-KLARNA` | `fnc` | `financingtype=KLV` | Klarna Invoice |

BillSAFE has been deprecated by PAYONE and is not supported. 

# API Data mapping

## TODO: ITEMS TO BE DISCUSSED

With PAYONE:

 * if a difference between the payment total and the line item totals occurs, Is it OK to just not set the price of the 
   line items or will that lead to a rejection from PAYONE? Background can be partial payments (e.g. a part paid with 
   a voucher) or the infamous absolute discount rounding issue. 
 * `sd[n]` "delivery date"  and `ed[n]` delivery end date fields: What is their meaning? 
 * `sequencenumber` / idempotenz von transaktionen. 
   * ist die sequencenumber bei direkter authorization (CT charge) 0 oder 1? 
   * bei capture kann man die sequence_number übergeben, das ist dann implizit indempotenz. 
 * Wie übersetzen wir price, receivable, balance in  amountPlanned, amountAuthorized, amountPaid
 * How to set `capturemode` -> NK discuss internally how to find out that a capture is the last delivery.
   * e.g. IF the sum of Charge transaction amounts incl. the now to do Charge equals amountPlanned, THEN it's the last? 
   * (allowed values: completed / notcompleted ). Mandatory just with Billsafe & Klarna.  -> 
 * What is the `bankbranchcode` and the ` bankcheckdigit` ? 
   * branch code is part of the bank identifier code, differs in length e.g. in France, Greece...
 * `protect_result_avs`  TODO when does this matter?
 * what does the bankcode have to do with sorting? 


With CT Product Management:

 * `customermessage` of an error -> was discussed at CT to add to PaymentStatus and was discarded until needed. Is needed now.
 * How to represent due amounts that are higher (or lower) than the initial amountPlanned?  (due to dunning and chargeback fees). 
 * The definiton of the amountAuthorized field is unclear: Is it the amount that is remaining (not yet charged) from the auth
   or is it just the sum of all successful authorizations? 
 * Give precise reasoning why payment changes should be processed through messages instead of querying for the transactionState.
 * do we have a working and robust Message endpoint listener implementation for Java? 
 * Stuff that should be built in (NKs Impression);
  * redirect / cancel / error / success URLs (redirectInfo Object)
  * reference Nr. (use key?)
  * Customer oriented status LText

## PAYONE fields that map to custom CT Payment fields

All payment methods:
   * _Required_ `reference`: should conventionally be the Order Number (assuming just one payment per Order). 
     The OrderNumber is only available on the CT Order, but not the CT Cart.
     Issue at hand: Checkout Implementations vary in respect to whether the Cart is converted into an Order before or after the Order is placed. 
     Proposed behavior:
     1. check if the Order is alredy created and has an Order Number. Take that as reference.
     1. If not: Create an Order Number and store it into a custom field `reference` in the Payment object. 
        An Integration Configuration determines the Custom _Object_ container and ID from which to get the next Order ID. 
        The Checkout implementation that creates Payment before Order then needs to assure that the Order ID
        is taken from the Payment Object if the Order is created after the Payment. 
   *  _Required_ `language` -> custom field `messageLocale` of Type String on the CT Payment
   * `redirecturl` ->  custom field `redirectUrl` of Type String on the CT Payment  (PAYONE master from response)
   * `successurl` ->  custom field `successUrl` of Type String on the CT Payment ( CT master )
   * `errorurl` -> custom field `errorUrl` Type String on Payment, CT master
   * `backurl`  -> custom field `canceUrl` Type String on Payment, CT master
   * `invoiceappendix` -> if a custom Field named `description` of Type String is set on the Cart / Order use that.  
 
 `DIRECT_DEBIT`*:
  * general:
    * `bankaccountholder` -> `accountHolderName` of type String
    * `narrative_text` -> `referenceText` of type String on the Payment
  * new data:
    * `iban` -> `IBAN` of type String
    * `bic`  -> custom `BIC` 
    * `mandate_identification` -> `sepaMandateId` of type String
    * `mandate_dateofsignature` ->  `sepaMandateDate` of type Date
  * traditional identification:
    * `bankcountry` -> `bankCountry` 
    * `bankaccount` -> `bankAccount` 
    * `bankcode` ->  TODO what does this have to do with sorting? 
    * `bankbranchcode` -> (only for FR, ES, FI, IT)
    * `bankcheckdigit` -> (only for FR, BE) 
 
 `BANK_TRANSFER`*:
  * `narrative_text` -> `referenceText` of type String on the Payment  
  * new data:
    * `iban` -> `IBAN` of type String
    * `bic`  -> custom `BIC` 
  * traditional identification:
    * `bankcountry` -> `bankCountry` 
 
  `BANK_TRANSFER-IDEAL`:
  * `bankgrouptype` -> custom field `bankGroupType` on Payment
  
  `BANK_TRANSFER-EPS`:
  * `bankgrouptype` -> custom field `bankGroupType on Payment

  `BANK_TRANSFER-SOFORTUEBERWEISUNG`:
  * 

  `BANK_TRANSFER-GIROPAY`:
  * 
 
 `CREDIT_CARD`*:
  * `pseudocardpan` -> `cardDataPlaceholder` of type String
  * `ecommercemode` ("internet" or "3dsecure") -> `force3DSecure` of type Boolean
  * TODO also allowed to save:  
    * Type / CC network
    * expiry date
    * truncated card number (passed back from PAYONE)
    * card holder name
  * `narrative_text` : text on the account statements -> `referenceText` of type String on the Payment  
 
 `INVOICE`*:
   * `invoiceid`  invoice ID (master in PAYONE).  PAYONE is master if invoice created by them.  For Klarna etc. CT data are master. 
   * `clearing_*` stuff -> written back from PAYONE.  Field names should be analogous to DIREC_DEBIT Bank data. 

 `INVOICE_KLARNA` (lots of mandatory risk management fields)
   * `personalid` -> Personal ID Nr.  Mandatory for Klarna if customers billing address is in certain nordics countries.     
   * `ip` -> the IP address of the user is not stored in CT. -> will need a custom field? (required for Klarna)

 Wallets:
  * `narrative_text` : text on the account statements -> `referenceText` of type String on the Payment     
   
## Fields not natively defined in CT, covered by custom Fields on Cart, Customer and Order

 * _Optional_ `customermessage`: Check for a custom Field `description` of type `LocalizedString` on the Cart / Order.
   Use the `locale` set on the Payment to pick the right value. 
 * _Optional_ `userid`: passed back from PAYONE as identification of the debtor account Nr.  If the CT Customer Object has a custom
    field named `payoneUserId` of type String, write the `userid` value into that field. 
  
The following are required only for Installment-Type Payment Methods (mainly Klarna): 
 
 * `birthdate` and `vatid` : these fields are only available on the CT Customer and not on the Cart. I.e. Guest checkouts
   cannot do some payment methods.
   * If the fields `dateOfBirth` (type Date) and `vatId` (type String) respectively are set as custom object on the 
     Cart / Order and have the right type they are used and take precedence over the Fields on the Customer Object
 * `gender`: Check the Cart for a custom field `customerGender`, as fallback check the Customer for a custom field 
    named `gender`. If the first existing is of Type `Enum` and has a value `Male` or `Female` -> use that one as `f` or `m` respectively.  
 * `ip`: Check for a custom Field `customerIPAddress` of type `String` on the CT Cart / Order. 
 
   
## unused PAYONE fields

 * `creditor_*`  just for debug? 
 * `bankcountry`, `bankaccount`, `bankcode`, `bic` (all replaced by the IBAN, which is preferable because it has a checksum)
 * `xid`, `cavv`, `eci` (3Dsecure is done via redirect only)   

 
## commercetools Payment resource

* [CT Payment documentation](http://dev.sphere.io/http-api-projects-payments.html#payment)

| CT payment JSON path | PAYONE Server API field | Who is master? |  Value transform | 
|---|---|---|---|
| id | (unused) | CT |  |
| version | (unused) | CT |  |
| customer.obj.id | `customerid` | CT | Use only as fallback to `.customerNumber` if that is not set. Extract first 20 non-dash characters to get a 20char string.  |
| customer.obj.customerNumber | `customerid` | CT | Log a Warning and ignore if the Number exceeds 20 characters. Do not truncate. |
| customer.obj.vatId | `vatId` | CT |  |
| customer.obj.dateOfBirth | `birthday` | CT | transform from ISO 8601 format (`YYYY-MM-DD`) to `YYYYMMDD`, i.e. remove the dashes |
| externalId | (unused, is intended for merchant-internal systems like ERP) | CT |  |
| interfaceId | `txid` | PAYONE |  |
| amountPlanned.centAmount | `price` | CT / PAYONE | Initially set by checkout, `price` from PAYONE notification must not deviate on Notifications. PAYONE value has to be multiplied by 100.  |
| amountPlanned.currency | - | CT |  |
| amountAuthorized.centAmount | TODO | PAYONE | TODO ask PAYONE  |
| authorizedUntil | TODO | PAYONE |  |
| amountPaid.centAmount | `receivable` minus `balance` | PAYONE | TODO verify with PAYONE |
| amountRefunded.centAmount | (from transactions) | PAYONE | (Sum of successful Refund Transactions) |
| paymentMethodInfo.paymentInterface | - | CT | Must be "PAYONE" in CT, otherwise do not handle the Payment at all |
| paymentMethodInfo.method | - | CT | (see the method mapping table above) |
| paymentMethodInfo.name.{locale} | - | - | (not passed, project specific content) |
| paymentStatus.interfaceCode | `errorcode` OR TODO | PAYONE | none |
| paymentStatus.interfaceText | `errormessage` | PAYONE | none |
| paymentStatus.state | - | - | (mapping from interfaceCode and transaction states to the Payment State Machine is project specific) |
| transactions\[\*\].id | - | CT (cannot be changed) |  |
| transactions\[\*\].timestamp | `txtime` | PAYONE | (from status notification) |
| transactions\[\*\].type |  |  | (see below for transaction types) |
| transactions\[\*\].amount.centAmount | `amount` | CT | none |
| transactions\[\*\].amount.currency | `currency` | CT | none, but must not deviate from amountPlanned.centAmount |
| transactions\[\*\].interactionId | `sequencenumber` | CT / PAYONE (must be in sync)  | There can be only one CT Authorization transaction. This must be the first and gets the sequencenumber 0. All following Charge, CancelAuthorization and Refund transactions count up from 1. TODO clarify Chargeback. TODO define Role in idempotency. TODO direct authorization is 0 or 1?  |
| transactions\[\*\].state | - | - | (see below for transaction states) |

See below for the custom fields. 

## Transaction Types and States

## PAYONE transaction types -> CT Transaction Types 

### triggering a new PAYONE transaction request given a CT transaction

* TODO how to trigger address / bank data checks? Just a a payment without transaction is not safe because the integration
  service processes the Payment Object change messages asynchronously and could miss the intermediate state without a transaction. 
  * request=bankaccountcheck / addresscheck / consumerscore  
  * As data check is not a financial transaction, we don't want to include it in the Transactions. A special Interaction? But interactions
    aren't supposed to be relevant to the checkout and frontend code. 
* TODO what to write into the CT payment object to trigger an `updatereminder`, i.e. dunning level trigger  

Please take care of idempotency. The `TransactionState` alone does not suffice to avoid creating duplicate PAYONE transactions. 
It could remain in `Pending` for various reasons.
`interfaceId`  and `timestamp` of the Transaction can be used to manage idempotency if a persistent field is necessary. 
 
| CT `TransactionType` | CT `TransactionState` | PAYONE `request` | Notes |
|---|---|---|---|
| `Authorization` | `Pending` | `preauthorization` |  |
| `CancelAuthorization` | `Pending` | ONLY on credit card: Send a capture with amount=0.   |  |
| `Charge` | `Pending` | if an `Authorization` Transaction exists: `capture`; otherwise: `authorization`  |  |
| `Refund` | `Pending` |  `debit` with negative amount of refund. `refund` is a subset of the functionality and does not need to be used.  |  |
| `Chargeback` | - | - |  (not applicable, is just triggered from PAYONE to CT)  |

### updating the CT Payment given a PAYONE TransactionStatus Notification

See chapter 4.2.1 "List of events (txaction)" and the sample processes in the PAYONE documentation

The matching transaction is found by sequencenumber = interactionId

* TODO when does `vauthorization` happen? What is it? 
* TODO when do we need the `managemandate` call? 
* TODO is explicit `3dscheck` (probably rather `check`?) necessary at all or implicit in the preauth/auth? 

[FH] PAYONE docu says that "you will receive the data and the status for each payment process". So maybe this table should incooperate also the CT PaymentState?
[FH] transaction_status seems to be only available with txaction "appointed" for now

| PAYONE `txaction` | PAYONE `transaction_status` | PAYONE `notify_version` | CT `TransactionType` | CT `TransactionState` | Notes |
|---|---|---|---|---|---|
| `appointed` | pending |  |  |  |  |
| `appointed` | completed |  |  |  |  |
| `capture` |  |  |  |  |  |
| `paid` |  |  |  |  |  |
| `underpaid` |  |  |  |  |  |
| `cancelation` |  |  |  |  | TODO is that  |
| `refund` |  |  |  |  |  |
| `debit` |  |  |  |  |  |
| `transfer` |  |  |  |  | Transfer like in "switch"/"move to" another bank account |
| `reminder` |  |  |  |  | status of dunning procedure, must be activated by PAYONE |
| `vauthorization` |  |  |  |  | only available with PAYONE Billing module, must be activated |
| `vsettlement` |  |  |  |  | only available with PAYONE Billing module, must be activated |
| `invoice` |  |  |  |  |  |
| `failed` | any | any | TODO all? charges? the one with the right sequence_number?  | `Failure` | (not fully implemented at PAYONE yet) |


## commercetools Cart and Order object (mapping to payment interface on payment creation)

* [CT Order documentation](http://dev.sphere.io/http-api-projects-orders.html#order)
* [CT Cart documentation](http://dev.sphere.io/http-api-projects-carts.html#cart)

TODO (important!): 
 * Migrate to new totals and discounted price fields (divide line item total by quantity to get best guess per qty price)

| CT Cart or Order JSON path | PAYONE Server API | who is Master?  | Value transform |
|---|---|---|---|
| id |  |  |  |
| createdAt |  |  |  |
| lastModifiedAt |  |  |  |
| customerId |  |  |  |
| customerEmail |  |  |  |
| country |  |  |  |
| totalPrice.currencyCode |  |  |  |
| totalPrice.centAmount |  |  |  |
| taxedPrice.totalNet.currencyCode |  |  |  |
| taxedPrice.totalNet.centAmount |  |  |  |
| taxedPrice.totalGross.currencyCode |  |  |  |
| taxedPrice.totalGross.centAmount |  |  |  |
| taxedPrice.taxPortions\[\*\].rate |  |  |  |
| taxedPrice.taxPortions\[\*\].amount.currencyCode |  |  |  |
| taxedPrice.taxPortions\[\*\].amount.centAmount |  |  |  |
| cartState | Active/Merged/Ordered |  |  |
| lineItems\[\*\] | `it[n]` | CT (existence of a line Item) | on lineItems the value is fixed to `goods` |
| lineItems\[\*\].id |  |  |  |
| lineItems\[\*\].name.{locale} | `de[n]` |  | truncate to 255 chars. TODO how to choose the right locale & fallback locale?  |
| lineItems\[\*\].quantity | `no[n]` | CT | fail hard if 3 chars length is exceeded |
| lineItems\[\*\].variant.id |  |  |  |
| lineItems\[\*\].variant.sku | `id[n]` | CT | truncate at 32 chars and warn |
| lineItems\[\*\].totalPrice.value.currencyCode |  |  |  |
| lineItems\[\*\].totalPrice.value.centAmount | `pr[n]` | CT | a) Divide this by .quantity to get the effective price per Line Item quantity. b) Round commercially to full cents. c) Add VAT vie .taxRate.amount if .taxRate.includedInPrice=false . d) Fail hard if 8 chars length is exceeded. e) Do not set `pr[n]` at all if the overall sum of `pr[n]` times `no[n]` does not equal the total of the payment. |
| lineItems\[\*\].taxRate.name |  |  |  |
| lineItems\[\*\].taxRate.amount | `va[n]` | CT | .amount is a float between zero and one. It has to be multiplied with 1000 and rounded to full integer to get base % points.|
| lineItems\[\*\].taxRate.includedInPrice |  |  | (required for calculating the price, see above)  |
| customLineItems\[\*\].id |  |  |  |
| customLineItems\[\*\].name.{locale} |  |  |  |
| customLineItems\[\*\].quantity |  |  |  |
| customLineItems\[\*\].money.currencyCode |  |  |  |
| customLineItems\[\*\].money.centAmount |  |  |  |
| customLineItems\[\*\].discountedPrice.value.currencyCode |  |  |  |
| customLineItems\[\*\].discountedPrice.value.centAmount |  |  |  |
| customLineItems\[\*\].taxRate.name |  |  |  |
| customLineItems\[\*\].taxRate.amount |  |  |  |
| customLineItems\[\*\].taxRate.includedInPrice |  |  |  |
| shippingAddress.title | (unused) |  |  |
| shippingAddress.salutation | (unused) |  |  |
| shippingAddress.firstName | `shipping_firstname` | CT |  |
| shippingAddress.lastName | `shipping_lastname` | CT |  |
| shippingAddress.streetName | `shipping_street` | CT |  |
| shippingAddress.streetNumber | `shipping_street` | CT | if set: append to .streetName, separated by space |
| shippingAddress.additionalStreetInfo | (unused) | CT |  |
| shippingAddress.postalCode | `shipping_zip` | CT |  |
| shippingAddress.city | `shipping_city` | CT |  |
| shippingAddress.region | (unused) |  |  |
| shippingAddress.state | `shipping_state` | CT | (only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN), Only if an ISO-3166-2 subdivision |
| shippingAddress.country | `shipping_country` | CT |  |
| shippingAddress.company | `shipping_company` | CT |  |
| shippingAddress.department | `shipping_company` | CT | if set: append to .company, separated with a commma |
| shippingAddress.building | (unused) |  |  |
| shippingAddress.apartment | (unused) |  |  |
| shippingAddress.pOBox | (unused) |  |  |
| shippingAddress.phone | (unused) |  |  |
| shippingAddress.mobile | (unused) |  |  |
| shippingAddress.email | (unused) |  |  |
| billingAddress.title | `title` | CT |  |
| billingAddress.salutation | `salutation` | CT |  |
| billingAddress.firstName | `firstname` | CT |  |
| billingAddress.lastName | `lastname` | CT |  |
| billingAddress.company | `company` | CT |  |
| billingAddress.streetName | `street` | CT |  |
| billingAddress.streetNumber | `street` | CT | if set: append to .streetName, separated with a space |
| billingAddress.additionalStreetInfo | `addressaddition` | CT |  |
| billingAddress.postalCode | `zip` | CT |  |
| billingAddress.city | `city` | CT |  |
| billingAddress.country | `country` | CT | both use ISO 3166 |
| billingAddress.state | `state` | CT | write to PAYONE only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN) and only if value is an ISO3166-2 subdivision |
| billingAddress.email | `email` | CT |  |
| billingAddress.phone | `telephonenumber` | CT |  |
| billingAddress.mobile | `telephonenumber` | CT | fallback value if .phone is not set |
| shippingInfo.shippingMethodName | `shippingprovider` | CT | any value starting with `DHL` is translated to `DHL` only. Any Value starting with `Bartolini` is translated to `BRT`  |
| shippingInfo.price.currencyCode |  |  |  |
| shippingInfo.price.centAmount |  |  |  |
| shippingInfo.taxRate.centAmount |  |  |  |
| shippingInfo.discountedPrice.price.currencyCode |  |  |  |
| shippingInfo.discountedPrice.price.centAmount |  |  |  |


## Payment Method specific fields of the payment object

Please use the commercetools custom payment types (per method) from the [method type specifications](https://github.com/nkuehn/payment-integration-specifications/blob/master/Method-Keys.md) in the payment specifications project. 

### CREDIT_CARD

#### commercetools payment object custom fields

`custom.fields.` has to be prefixed when actually accessing these fields.  

| CT Payment custom property | PAYONE Server API | Who is Master? | Value transform |
|---|---|---|---|
| foo |  |  |  |
| foo |  |  |  |

### DIRECTDEBIT_SEPA

XXXX ... analogous to the Credit Card sample above ... 

# Constraint Rules to be implemented by the Integration

* If payment method INSTALLMENT-KLARNA or BILLSAFE are used, billing address and 
delivery address must be identical. 

