modules = ["java", "python-3.11"]

[nix]
channel = "stable-24_05"

[workflows]
runButton = "Project"

[[workflows.workflow]]
name = "Project"
mode = "parallel"
author = "agent"

[[workflows.workflow.tasks]]
task = "workflow.run"
args = "Android Studio"

[[workflows.workflow.tasks]]
task = "workflow.run"
args = "java_app"

[[workflows.workflow]]
name = "Android Studio"
author = "agent"

[workflows.workflow.metadata]
agentRequireRestartOnSave = false

[[workflows.workflow.tasks]]
task = "packager.installForAll"

[[workflows.workflow.tasks]]
task = "shell.exec"
args = "./gradlew installDebug && adb shell am start -n com.ktu.timetable/.SplashActivity"
waitForPort = 5000

[[workflows.workflow]]
name = "java_app"
author = "agent"

[workflows.workflow.metadata]
agentRequireRestartOnSave = false

[[workflows.workflow.tasks]]
task = "packager.installForAll"

[[workflows.workflow.tasks]]
task = "shell.exec"
args = "./gradlew run"

[deployment]
run = ["sh", "-c", "./gradlew installDebug && adb shell am start -n com.ktu.timetable/.SplashActivity"]
