# Migration Guide

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Migrating from v1 to v2](#migrating-from-v1-to-v2)
  - [1. Integration Service changes](#1-integration-service-changes)
  - [2. Payone portal changes](#2-payone-portal-changes)
  - [3. Changes in the shops which uses the service](#3-changes-in-the-shops-which-uses-the-service)
  - [4. Setup a new tenant (branch,shop,merchant)](#4-setup-a-new-tenant-branchshopmerchant)
  - [5. PROFIT](#5-profit)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Migrating from v1 to v2

Version 2 introduced multi-tenancy feature. This is a ***breaking-change*** thus requires additional steps in the 
service and Payone provider setup.

**Note**: after version 2 release the first version likely will be abandoned hence we recommend to do the migration as 
soon as it is possible for your project.

If you are setting up the project from scratch - likely you should just follow [Multitenancy](/README.md#multitenancy) 
section in the main documentation page.

### 1. Integration Service changes

1. One mandatory property added: `TENANTS`, a comma (or semicolon) separate list of your tenants (branches, franchising etc).
This value will be used as a part of URL path for handling and notification resources. 

    If you have exactly one tenant - put a single name. For example:
    ```
    TENANTS=MAIN-SHOP
    ```
    
    Read [Mandatory common properties](/README.md#mandatory-common-properties) section for more details 
    and the names restrictions.

1. Add tenant name as a prefix to tenant-specific environment variables/properties 
(note, underscore **_** is required between the prefix and variable name):

Old variable name               | New variable name (for single tenant with name `MAIN-SHOP` | Mandatory
--------------------------------|------------------------------------------------------------| ---------
`CT_PROJECT_KEY`                | `MAIN-SHOP_CT_PROJECT_KEY`                                 | **Yes**
`CT_CLIENT_ID`                  | `MAIN-SHOP_CT_CLIENT_ID`                                   | **Yes**
`CT_CLIENT_SECRET`              | `MAIN-SHOP_CT_CLIENT_SECRET`                               | **Yes**
`CT_START_FROM_SCRATCH`         | `MAIN-SHOP_CT_START_FROM_SCRATCH`                          | No
`PAYONE_KEY`                    | `MAIN-SHOP_PAYONE_KEY`                                     | **Yes**
`PAYONE_MERCHANT_ID`            | `MAIN-SHOP_PAYONE_MERCHANT_ID`                             | **Yes**
`PAYONE_MODE`                   | `MAIN-SHOP_PAYONE_MODE`                                    | **Yes**
`PAYONE_PORTAL_ID`              | `MAIN-SHOP_PAYONE_PORTAL_ID`                               | **Yes**
`PAYONE_SUBACC_ID`              | `MAIN-SHOP_PAYONE_SUBACC_ID`                               | **Yes**
`PAYONE_MODE`                   | `MAIN-SHOP_PAYONE_MODE`                                    | No
`UPDATE_ORDER_PAYMENT_STATE`    | `MAIN-SHOP_UPDATE_ORDER_PAYMENT_STATE`                     | No
`SECURE_KEY`                    | `MAIN-SHOP_SECURE_KEY`                                     | No

`SHORT_TIME_FRAME_SCHEDULED_JOB_CRON` and `LONG_TIME_FRAME_SCHEDULED_JOB_CRON` are common to the whole service 
thus do not require tenant specific configuration.

### 2. Payone portal changes

In Payone go to [Payment portals](https://pmi.pay1.de/merchants/?navi=portal&list=1) page, edit portal with id 
`PAYONE_PORTAL_ID` and:

Tab        | Field name              | Old value                                                   | New value | Comment
-----------|-------------------------|-------------------------------------------------------------|-----------------------------------------------------------------------------------------|---------
_Extended_ | `TransactionStatus URL` | `https://payone-integration.myshop.com/payone/notification` | <code>https://payone-integration.myshop.com/<b>MAIN-SHOP</b>/payone/notification</code> | **Mandatory**
_General_  | `URL`                   | `https://payone-integration.myshop.com`                     | <code>https://payone-integration.myshop.com/<b>MAIN-SHOP</b>                            | Optional

### 3. Changes in the shops which uses the service

After you setup the service and respective Payone portal - update a shop settings. 
Find a place where URL request the integration service is configured. 
You have to change the payment handling URL:
  1. The old URL expected to be a string containing `/commercetools/handle/payments/`
  1. Insert your default tenant name (like `MAIN-SHOP`) into this path: <code>**MAIN-SHOP**/commercetools/handle/payments/</code>

### 4. Setup a new tenant (branch,shop,merchant)

  1. Add a new tenant name to `TENANTS`, like `TENANTS=MAIN-SHOP,MERCHANT1`
  1. [Add a new portal in Payone](https://pmi.pay1.de/merchants/?navi=portal) with `MERCHANT1` path part in the notification URL
  1. [Add a new commercetools platform account](https://admin.commercetools.com/) 
  1. In the service settings (environment variables) add the same properties, but with `MERCHANT1_` prefix instead of `MAIN-SHOP_`
  1. In the new shop configure payment checkout service to connect to the new merchant URL 
    (like <code>**MERCHANT1**/commercetools/handle/payments/</code>)

### 5. PROFIT

Congratulations, you are done!