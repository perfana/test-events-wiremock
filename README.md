# test-events-wiremock

Events to load and change wiremock stubs during load tests.

Properties:
* `wiremockFilesDir` the directory where to find the wiremock files
* `wiremockUrl` the wiremock urls, comma separated
* `useProxy` on port 8888, for example to use with fiddler

Custom events:
* `wiremock-change-delay` use to change delay of wiremock instances at specific time

Example wiremock response with a dynamic delay:

```json
{
  "request": {
    "method": "GET",
    "url": "/delay"
  },
  "response": {
    "status": 200,
    "body": "Hello world! from Wiremock, with delay :-)",
    "fixedDelayMilliseconds": ${delay},
    "headers": {
      "Content-Type": "text/plain"
    }
  }
}
```
Put this in a file at in the files dir and it gets uploaded with the specific delay.
The `$delay` will be replaced by the values in the `eventSchedulerScript`.

You can define multiple delays in multiple mapping files.

Define the delays in a `eventSchedulerScript`, example in use with events-gatling-maven-plugin:

```xml
<eventSchedulerScript>
    PT0S|wiremock-change-delay-fast|delay=400
    PT30S|wiremock-change-delay-slow|delay=4000
    PT1M30S|wiremock-change-delay-really-slow|delay=8000
</eventSchedulerScript>
```
This means: set delay to 400 milliseconds at the start of the Gatling load test.
Then increase the response time to 4000 milliseconds after 30 seconds.
And increase response time to 8000 milliseconds after 1 minute and 30 seconds.

The second part (`wiremock-change-delay-fast`) is an event label that will be attached to this event and made visible
in Perfana and in graphs.

To replace multiple delays, use multiple replace tags in your mapper files,
e.g. `${delay-1}`, `${delay-2}`, `${delay-3}`, you can use a `;` separator in one event line:

    PT30S|wiremock-change-delay|delay-1=4000;delay-2=3000;delay-3=2000

## Use with events-*-maven-plugin

You can use the `test-events-wiremock` as a plugin of the `events-*-maven-plugin`
by putting the `test-events-wiremock` jar on the classpath of the plugin.

You can use the `dependencies` element inside the `plugin` element.

For example (from [example-pom.xml](src/test/resources/example-pom.xml)):

```xml
<plugin>
    <groupId>io.perfana</groupId>
    <artifactId>event-scheduler-maven-plugin</artifactId>
    <configuration>
        <eventSchedulerConfig>
            <debugEnabled>true</debugEnabled>
            <schedulerEnabled>true</schedulerEnabled>
            <failOnError>true</failOnError>
            <continueOnEventCheckFailure>true</continueOnEventCheckFailure>
            <scheduleScript>
                ${eventScheduleScript}
            </scheduleScript>
            <eventConfigs>
                <eventConfig implementation="io.perfana.event.wiremock.WiremockEventConfig">
                    <name>WiremockEvent1</name>
                    <wiremockFilesDir>src/test/resources/wiremock-stubs</wiremockFilesDir>
                    <wiremockUrl>http://localhost:9999</wiremockUrl>
                    <useProxy>false</useProxy>
                    <testConfig>
                        <systemUnderTest>${systemUnderTest}</systemUnderTest>
                        <version>${version}</version>
                        <workload>${workload}</workload>
                        <testEnvironment>${testEnvironment}</testEnvironment>
                        <testRunId>${testRunId}</testRunId>
                        <buildResultsUrl>${buildResultsUrl}</buildResultsUrl>
                        <rampupTimeInSeconds>${rampupTimeInSeconds}</rampupTimeInSeconds>
                        <constantLoadTimeInSeconds>${constantLoadTimeInSeconds}</constantLoadTimeInSeconds>
                        <annotations>${annotations}</annotations>
                        <tags>${tags}</tags>
                    </testConfig>
                </eventConfig>
            </eventConfigs>
        </eventSchedulerConfig>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.perfana</groupId>
            <artifactId>test-events-wiremock</artifactId>
            <version>${test-events-wiremock.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

You can substitute `event-scheduler-maven-plugin` by `event-gatling-maven-plugin`, `event-jmeter-maven-plugin`
and others when available.

Try this by calling:

    java -jar wiremock-standalone-[version].jar -v --port 9999

and

    mvn -f src/test/resources/example-pom.xml event-scheduler:test


Works with the Perfana event-scheduler framework: 
* https://github.com/perfana/event-scheduler
* https://github.com/perfana/events-gatling-maven-plugin