# test-events-wiremock

Events to load and change wiremock stubs during load tests.

Properties:
* `wiremockFilesDir` the directory where to find the wiremock files
* `wiremockUrl` the wiremock base url, e.g. `http://wiremock:9999/`
* `useProxy` true/false calls localhost proxy on port 8888, for example to use with mitmproxy
* `continueOnUploadError` if true, continue uploading other files if an upload error occurs, default is true

Custom events:
* `wiremock-change-mappings` --- change delay of wiremock mapping file(s)
  * uses the `/__admin/mappings` endpoint
* `wiremock-change-settings` --- change delay of wiremock settings file
  * uses the `/__admin/settings` endpoint
* `wiremock-change-import` --- change delay of wiremock import file 
  * uses the `/__admin/mappings/import` endpoint

Use the correct type of file for each event. For import use the exported file of wiremock studio
that contains multiple mappings.

All files should end in `.json`.

Example wiremock response (single mapping) with a dynamic delay:

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
Put this in a mapper file located in the `wiremockFilesDir` and it gets uploaded with 
the specific delay. The `$delay` will be replaced by the values in the `eventSchedulerScript`.

You can define multiple delays in multiple mapping files.

Define the delays in a `eventSchedulerScript`, example in use with events-gatling-maven-plugin:

```xml
<eventSchedulerScript>
    PT0S|wiremock-change-mappings(fast)|delay=400
    PT30S|wiremock-change-mappings(slow)|delay=4000
    PT1M30S|wiremock-change-mappings(really-slow)|delay=8000
</eventSchedulerScript>
```
This means: set delay to 400 milliseconds at the start of the Gatling load test.
Then increase the response time to 4000 milliseconds after 30 seconds.
And increase response time to 8000 milliseconds after 1 minute and 30 seconds.

The second part (`wiremock-change-mappings(fast)`) is an event name plus annotation that 
is sent as event annotation as made visible in Perfana and in graphs.

To replace multiple delays, use multiple replace tags in your mapper files,
e.g. `${delay-1}`, `${delay-2}`, `${delay-3}`, you can use a `;` separator in one event line:

    PT30S|wiremock-change-mappings|delay-1=4000;delay-2=3000;delay-3=2000

## specific file

When no `file` or `directory` is specified, all `.json` files in the `wiremockFilesDir` will have the
replacements applied and will be uploaded.

To just upload one specific file, use the file parameter. Example, only the `wiremock-settings.json` will be
processed and replaced:

```
PT10S|wiremock-change-settings|file=wiremock-settings.json;delay=400
```

## directories

Instead of using replacements, you can also create multiple directories that contain
the `mappings`, `settings` or `import` files. By pointing to different directories at certain times,
the files in that directory will be loaded. 


Example schedules:

### mappings

First the mappings will be deleted and replaced,
so you can use totally different mappings if needed.

```xml
<eventSchedulerScript>
  PT8S|wiremock-change-mappings|directory=my-mappings-dir-fast
  PT13S|wiremock-change-mappings|directory=my-mappings-dir-slow
  PT38S|wiremock-change-mappings|directory=my-mappings-dir-fast
</eventSchedulerScript>
```

### imports

Import files will also replaced, the imported mapping are not deleted.
The uuid's of the mappings need to be the same in each file for this to succeed.

```xml
<eventSchedulerScript>
  PT13S|wiremock-change-import|directory=my-imports-dir-fast
  PT25S|wiremock-change-import|directory=my-imports-dir-slow
  PT50S|wiremock-change-import|directory=my-imports-dir-errors
  PT59S|wiremock-change-import|directory=my-imports-dir-fast
</eventSchedulerScript>
```

### settings

The settings file will be replaced.

```xml
<eventSchedulerScript>
  PT0S|wiremock-change-settings|directory=my-settings-dir-fast
  PT5S|wiremock-change-settings|directory=my-settings-dir-slow
  PT30S|wiremock-change-settings|directory=my-settings-dir-fast
</eventSchedulerScript>
```

Make sure the directories are sub-directories of the `wiremockFilesDir`.


## use proxy

Use a proxy like [mitmproxy](https://mitmproxy.org/) to debug the http traffic between the 
plugin and wiremock. Set `useProxy` to true and use activate proxy on `http://localhost:8888`.

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
                    <wiremockUrl>http://localhost:9999/__admin/mappings</wiremockUrl>
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