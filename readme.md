<p>
<img src="https://img.shields.io/badge/license-GPLv2%20CE-green?style=plastic" alt="license"/>
<img src="https://img.shields.io/badge/java-17+-yellowgreen?style=plastic" alt="java version"/>
<a href="https://central.sonatype.com/search?smo=true&q=modeler&namespace=io.github.zenliucn.domain">
<img src="https://img.shields.io/maven-central/v/io.github.zenliucn.domain/parent?style=plastic" alt="maven central"/>
</a>
</p>

# Domain

Utilities and implements for Domains

## License

License as `GPL v2 with classpath exception`.

## module

1. modeler:  prototype and APT for domain design.

### modeler
a toolset for domain design.
```xml
<dependecies>
    <dependecy>
        <groupId>io.github.zenliucn.domain</groupId>
        <artifactId>modeler</artifactId>
        <version>0.1.0</version>
    </dependecy>
    <!-- required for use ModelerProcessor -->
    <dependency>
        <groupId>com.squareup</groupId>
        <artifactId>javapoet</artifactId>
        <version>1.13.0</version>
        <optional>true</optional>
        <scope>provided</scope>
    </dependency>
</dependecies>
```
![model](modeler/model.svg)
