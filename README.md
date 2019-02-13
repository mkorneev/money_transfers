Implementation of a RESTful API for money transfers between accounts.

##### Requirements

1. You can use Java, Scala or Kotlin.
2. Keep it simple and to the point (e.g. no need to implement any authentication).
3. Assume the API is invoked by multiple systems and services on behalf of end users.
4. You can use frameworks/libraries if you like (except Spring), but don't forget about
requirement #2 – keep it simple and avoid heavy frameworks.
5. The datastore should run in-memory for the sake of this test.
6. The final result should be executable as a standalone program (should not require
a pre-installed container/server).
7. Demonstrate with tests that the API works as expected.

##### Tech Stack

Kotlin, Spark-Kotlin, Spock, Arrow, Scala-STM, DDD


##### How to Run

    ./gradlew run --args <port>
    
    
Get test coverage by running

    ./gradlew clean build jacocoTestReport && open build/reports/jacoco/test/html/index.html
    
   
#### API Documentation

See documentation at [docs/api](docs/api/index.md)

#### Performance Test Results

    Server Software:        Jetty(9.4.4.v20170414)
    Server Hostname:        localhost
    Server Port:            4567
    
    Document Path:          /api/transfer
    Document Length:        180 bytes
    
    Concurrency Level:      10
    Time taken for tests:   1.363 seconds
    Complete requests:      10000
    Failed requests:        0
    Total transferred:      3070000 bytes
    Total body sent:        2650000
    HTML transferred:       1800000 bytes
    Requests per second:    7338.77 [#/sec] (mean)
    Time per request:       1.363 [ms] (mean)
    Time per request:       0.136 [ms] (mean, across all concurrent requests)
    Transfer rate:          2200.20 [Kbytes/sec] received
                            1899.19 kb/s sent
                            4099.39 kb/s total
    
    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    1   1.9      0     103
    Processing:     0    1   1.6      1      58
    Waiting:        0    1   1.4      1      58
    Total:          1    1   2.5      1     103

