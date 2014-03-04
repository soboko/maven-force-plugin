#Force.com Maven Plugin

The Force.com Maven plugin enables you to generate Force.com JPA POJOs based on the objects already present in your Force.com organization.

##Configuring a Force.com Connection
All plugin configurations require a Force.com connection name. We recommend using an environment variable for configuration. The environment variable name depends on the connection name. For more about connection names, see the [Database.com Java SDK](http://forcedotcom.github.com/java-sdk/connection-url).

For example, if your connection name is `connName`, the associated environment variable must be `FORCE_CONNNAME_URL`.

On Mac or Linux:

<pre>
    <code>
    export FORCE_CONNNAME_URL=force://login.salesforce.com\;user=<em>username</em>\;password=<em>password</em>
    </code>
</pre>

**Note**: Each semi-colon must be escaped with a backslash.

On Windows:

<pre>
    <code>
    set FORCE_CONNNAME_URL=force://login.salesforce.com;user=<em>username</em>;password=<em>password</em>
    </code>
</pre>

##Basic Configuration
The basic configuration requires a Force.com connection name that is referenced in the `<connectionName>` element in a `pom.xml` file.

    <plugin>
      <groupId>com.force</groupId>
      <artifactId>maven-force-plugin</artifactId>
      <version>22.0.5-BETA</version>
      <configuration>
        <all>true</all>
        <connectionName>connname</connectionName>
      </configuration>
    </plugin>
    
**Note**: For an explanation of the `<all>` element, see the next configuration section.

##Configuring Force.com JPA Entities for Code Generation
To configure POJO generation, add the following to your `pom.xml` file under the Force.com Maven plugin:

    <executions>
      <execution>
        <id>generate-force-entities</id>
        <goals>
          <goal>codegen</goal>
        </goals>
      </execution>
    </executions>

To generate POJOs for all objects in your organization, edit the `<configuration>` element to include `<all>true</all>`:

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
    </configuration>
    
To only include certain objects, use separate `<include>` elements instead:

    <configuration>
      <connectionName>connname</connectionName>
      <includes>
        <include>Account</include>
        <include>Contact</include>
        ...
      </includes>
    </configuration>

By default, the plugin follows all object references and generates all the necessary files so that generated source will compile. For example, the standard Contact entity has a relationship field to the Account entity. If you generate a Java class for the Contact entity, the code generator generates both Contact and Account classes, as well as classes for any other relationships for Contact.

If you don't want to follow object references, set the `<followReferences>` element to `false`. You can use `<include>` elements for any references that you want to follow.

    <configuration>
      <connectionName>connname</connectionName>
      <followReferences>false</followReferences>
      <includes>
        <include>Account</include>
        <include>Contact</include>
        ...
      </includes>
    </configuration>

To exclude certain objects:

    <configuration>
      <connectionName>connname</connectionName>
      <excludes>
        <exclude>Opportunity</exclude>
        <exclude>CustomObject__c</exclude>
        ...
      </excludes>
    </configuration>

This configuration generates classes for all objects excluding those listed in an `<exclude>` element.

**Note**: Your `<configuration>` element must include one of the following elements:

* `<all>true</all>`
* `<includes>`
* `<excludes>`
* `<inclusionConfigFile>` (see below)

The default directory for generated Java source files is `src/main/java`. You can override the default by defining a `<destDir>` element.

The default Java package name is `com.<orgNameDenormalized>.model`, where `<orgNameDenormalized>` is an identifier that is automatically created from your organization name. You can override the default by defining a `<packageName>` element.

    <configuration>
      <all>true</all>
      <connectionName>connname</connectionName>
      <destDir>${basedir}/src/main/java</destDir>
      <packageName>com.mycompany.package.name</packageName>
    </configuration>
    
**Important Note**: If you plan on creating Force.com schema through the JPA provider, you should only ever run JPA code generation *once*.  Failing to do this
might result in conflicts among your Java classes if you manually change the generated code and then run code generation again. If you plan on managing Force.com schema outside of the JPA provider, then you may run JPA code generation as many times as you like.

### Using a HOCON (typesafe config) configuration

#### Format

You may want to have finer-grained control over exactly which fields are generated for each object. You can use a [HOCON configuration file](https://github.com/typesafehub/config/blob/master/HOCON.md) to do this. Here is an example:

```
Account = [Legacy_User_Id__c, FirstName, LastName, PersonEmail, RecordTypeId,
           PersonLeadSource, AccountSource, Account_Status__c, Phone, PersonMailingCity,
           PersonMailingState, PersonMailingPostalCode]
Contact = [FirstName, LastName, Email]
Opportunity = [Account, RecordType, Name, StageName, Type, CloseDate, LeadSource]
OpportunityLineItem = [Opportunity, PricebookEntry, Quantity, TotalPrice, UnitPrice, ListPrice]
```
Pretty simple stuff, really. The value before the equals sign is an object name. Any underscores in the object name are ignored during the matching process (the same goes for an "__c" suffix at the end of the name). Values enclosed in square brackets after the equals sign indicate the fields to include in the generated object. You can achieve very fine-grained control over what you are generating, so that you only generate the objects and fields that you really need.

But wait, there's more! You can also take advantage of wildcards and exclusions. Perhaps you have an object where you want to include every field, but you're too lazy to type them all in:

```
Package = [_]
```
Or maybe you have objects where you want to include everything except for one or two fields. This is where the ~ symbol come in. Any field starting with ~ will not be included in the generated class, so in the example below, CurrencyIsoCode will not be included:

```
NegativeLinks = [_, ~CurrencyIsoCode]
```
You can also specify inclusions or exclusions that apply to every object. For instance, you could put this line at the top of your configuration file:

```
_ = [Name, ~ConnectionReceived, ~ConnectionSent]
```
This will ensure that the name field is included for every object that has a name, and that the ConnectionReceived and ConnectionSent fields will be excluded from the objects that have those fields defined.

Here's an example that puts it all together:

```
// globals

_ = [Name, ~ConnectionReceived, ~ConnectionSent]

// objects

Account = [Legacy_User_Id__c, FirstName, LastName, PersonEmail, RecordTypeId,
           PersonLeadSource, AccountSource, Account_Status__c, Phone, PersonMailingCity,
           PersonMailingState, PersonMailingPostalCode]

ChargentBaseGateway = [Id]
Contact = [FirstName, LastName, Email]
NegativeLinks = [_, ~CurrencyIsoCode]

##
Opportunity = [Account, RecordType, Name, StageName, Type, CloseDate, LeadSource]
OpportunityLineItem = [Opportunity, PricebookEntry, Quantity, TotalPrice, UnitPrice, ListPrice]
Package = [_]
RecordType = [_, ~BusinessProcess]
Task = [_]

User = [username, lastName, firstName, name, companyName, division, department, street,
        city, state, postalCode, country, email, phone, fax, mobilePhone, alias, isActive]
```
#### Required Maven Configuration

To make use of this feature, just specify a valid configuration file in the **inclusionConfigFile** option, as shown here:

```
<configuration>
    <connectionName>con</connectionName>
    <inclusionConfigFile>${basedir}/src/main/resources/objects.conf</inclusionConfigFile>
    <destDir>${project.build.directory}/generated-sources/salesforce</destDir>
    <packageName>com.soboko.salesforce.model</packageName>
</configuration>

```


##Generating Force.com JPA Entities
After you have configured your `pom.xml` file, generate JPE entities by running:

    mvn force:codegen

###Generating Force.com JPA Entities on Heroku
Heroku does not allow environment variables at build time.  This means that your Force.com database credentials will not be available at
build time for the JPA code generation to run.  There are two options:

1. Check in Force.com database credentials (*not recommended*)
2. Pre-run the JPA code generation and check in the resulting source code files.

The latter requires an opt-in strategy for running code generation.  Here's an example:

    <profile>
      <id>force-codegen</id>
      <activation>
        <property>
          <name>forceCodeGen</name>
          <value>true</value>
        </property>
      </activation>
      
      <build>
        <plugins>
        
          <plugin>
            <groupId>com.force</groupId>
            <artifactId>maven-force-plugin</artifactId>
            <version>22.0.5-BETA</version>
            <executions>
              <execution>
                <id>generate-force-entities</id>
                <goals>
                  <goal>codegen</goal>
                </goals>
              </execution>
            </executions>
            <configuration>
              <all>true</all>
              <connectionName>connname</connectionName>
              <destDir>${basedir}/src/main/java</destDir>
              <packageName>com.mycompany.package.name</packageName>
            </configuration>
          </plugin>
          
        </plugins>
      </build>
    </profile>
    
Now you can opt-in to code generation with the following:

    mvn clean install -DskipTests -DforceCodeGen

##Build
The build requires Maven version 2.2.1 or higher.

    mvn clean install -DskipTests

##Run Tests
First mark the `force-test-connection.properties` file to be ignored by git:

    git update-index --assume-unchanged src/test/resources/force-test-connection.properties
    
This follows our recommended best practices of not checking authentication credentials into source control.    

Add Force.com database credentials to the `force-test-connection.properties` file and run:

    mvn test
