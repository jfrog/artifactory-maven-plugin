# 📖 Guidelines

- If the existing tests do not already cover your changes, please add tests.

# ⚒️ Building the plugin

To build the plugin sources, please follow these steps:

1. Clone the code from Git.
2. Install the plugin by running the following Maven command:

```
mvn clean install
```

# 🧪 Testing the plugin

* If you'd like to run the plugin's tests, run the following commands:

```
mvn clean install
mvn verify -DskipITs=false
```

The above command run both unit and integration tests.

* In order to remote debug the integration tests, add `debugITs=true` property to the test command and add a break point
  in your IDE:

```
mvn clean install
mvn verify -DskipITs=false -DdebugITs=true
```

After running the above command, you should start a remote debugging session on port 5005 and wait until the code
reaches the break point.
