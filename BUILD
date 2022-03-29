
#======================================================================
# Bare-bones Bazel BUILD file for MyPL
# CPSC 321
# Spring, 2022
#======================================================================

load("@rules_java//java:defs.bzl", "java_test")

java_binary(
  name = "mypl",
  srcs = glob(["src/*.java"]),
  main_class = "MyPL",
)

java_library(
  name = "mypl-lib",
  srcs = glob(["src/*.java"]),
)

#----------------------------------------------------------------------
# TEST SUITES:
#----------------------------------------------------------------------

java_test(
    name = "code-generator-test",
    srcs = ["tests/CodeGeneratorTest.java"], 
    test_class = "CodeGeneratorTest",
    deps = ["lib/junit-4.13.2.jar", "lib/hamcrest-core-1.3.jar", ":mypl-lib"],
)




