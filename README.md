## Reliability patterns at Glovo

Welcome to the reliability patterns workshop! 

This readme will help you to set up your dev environment to implement
the reliability patterns we've just seen in the previous session.
The goal of this workshop is to implement a simple version of the following
reliability patterns: 

* Fallback
* Timeout
* Retry
* Circuit breaker

The scenario is simple. You need to implement a class that provides methods to 
reliability fetch orders from the orders service. The client could be the Customer Profile service,
which would need to fetch the list of orders for a customer in order to display
it as part of the customer profile. 

[](./img/scneario.png =250x)


To implement these patterns you will need to implement the methods from the 
[OrdersFetcher](./lib/src/main/java/stability/OrdersFetcher.java) interface using 
the [ReliableOrdersFetcher](./lib/src/main/java/stability/ReliableOrdersFetcher.java) class.
You will need to provide a reliable implementation 




