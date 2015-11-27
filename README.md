# commercetools <-> PAYONE Integration Service

[![Build Status](https://travis-ci.com/sphereio/commercetools-payone-integration.svg?token=BGS8vSNxuriRBqs9Ffzs&branch=master)](https://travis-ci.com/sphereio/commercetools-payone-integration)

This software provides an integration between the [commercetools eCommerce platform](http://dev.sphere.io) API
and the [PAYONE](http://www.payone.de) payment service provider API. 

It is a standalone Microservice that connects the two cloud platforms and provides own helper APIs to checkout
implementations. 
 
(remove after public:) There is a chat room concerning all things here https://www.hipchat.com/gWjfePSzk 
 
## Using the Integration in a project

### Required Configuration in commercetools

fooBar (add types, etc pp). 

### Required Configuration in PAYONE

https://pmi.pay1.de/

 * create a Payment Portal of type "Shop" for the site you are planning (please also maintain separate portal for 
   automated testing, demo systmes etc.)
 * set the hashing algorithm to sha2-384  ("advanced" tab in the portal config)
 * put the notification listener URL of where you will deploy the microservice into "Transaction Status URL" in the 
   "advanced" tab of the portal
 * configure the "riskcheck" settins as intended (esp. 3Dsecure)

### Configuration Options of the Integration itself

fooBar

### Deploy and Run

fooBar docker and heroku options

## Contribute Improvements

If you want to add a useful functionality or found a bug please open an issue here to announce and discuss what you
have in mind.  Then fork the project somewhere or in GitHub and create a pull request here once you're done. 

fooBar (target group are solution implementors using the integration)

## Create a custom version

Just fork it. The MIT License allows you to do anything with the code, commercially or noncommercial.
 