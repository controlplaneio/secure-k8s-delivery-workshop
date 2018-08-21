// based on @sublimino's canonical seed job, lightly modified by @lukebond

env = System.getenv()

def projects = [
  [
    domain              : "github.com",
    org                 : "YOUR-ACCOUNT-HERE",// change this to your github account
    name                : "demo-api",         // git repo name
    jobName             : "hello-world",      // jenkins job name
    branch              : "master",
    jenkinsfile         : "Jenkinsfile",
    pollTrigger         : "H/5 * * * *",
    cronTrigger         : "",
    depth               : 1,
    environmentWhitelist: [],
    image               : "latest",
    disabled            : false,
    permissions         : [
        "hudson.model.Item.Build:Developer",
        "hudson.model.Item.Cancel:Developer",
        "hudson.model.Item.Delete:Developer",
        "hudson.model.Item.Read:Developer"
    ]
  ]
]

print "\n\nProjects:\n\n${projects}\n\n"

// ---

// depth denotes how many of the following to create
// if environmentWhitelist is populated only those listed may be created
def environments = [
    [name: "Dev"],
    [name: "CI"],
    [name: "QA"],
    [name: "Stg"],
    [name: "Pre"],
    [name: "Prod"]
]

def isInTest() {
  env = System.getenv()
  return (env['JENKINS_DSL_OVERRIDE'] && env['JENKINS_DSL_OVERRIDE']?.trim() != "")
}

def isLocalJobOverride() {
  return (env['JENKINS_LOCAL_JOB_OVERRIDE'] && env['JENKINS_LOCAL_JOB_OVERRIDE']?.trim() != "")
}

def isLocalFilePath(path) {
  return path && path.substring(0, 7) == 'file://'
}

def getJobName(project) {
//  if (isLocalFilePath(project.localPathOverride)) {
//    return project.localPathOverride.split('/').last()
//  }
  return "${project.jobName ? project.jobName : project.name}"
}

def getRepoUrl(project) {
  if (!project.name) {
    throw new Exception("Name required in project")
  }
  if (isInTest() && isLocalJobOverride() && isLocalFilePath(project.localPathOverride)) {
    return project.localPathOverride
  }
  if (project.url) {
    return project.url
  }
  return "ssh://git@${project.domain}/${project.org}/${project.name}.git"
}

// ---

def getJenkinsfileName(pipeline, stage = '') {
  if (pipeline.jenkinsfile) {
    fileName = pipeline.jenkinsfile
  } else {
    fileName = 'Jenkinsfile'
  }
  return fileName + (stage ? ".${stage}" : '')
}

def getBranch(pipeline = "") {
  if (isLocalJobOverride() || pipeline == "") {
    return '**'
  }
  if (pipeline.branch) {
    return pipeline.branch
  }
  return '*/master'
}

def getPollTrigger(pipeline = "") {
  if (isInTest()
    || pipeline == ""
    || pipeline.pollTrigger == ''
  ) {
    return ''

  } else if (pipeline.pollTrigger != null) {
    return pipeline.pollTrigger
  }

  return '* * * * *'
}

def getCronTrigger(pipeline = "") {
  if (isInTest() || pipeline == "") {
    return ''

  } else if (pipeline.cronTrigger) {
    return pipeline.cronTrigger
  }

  return ""
}

// ------------

defaultListColumns = {
  status()
  weather()
  name()
  lastSuccess()
  lastFailure()
  lastDuration()
  lastBuildConsole()
  progressBar()
  buildButton()
}

buildMonitorView("0-buildmon") {
  jobs {
    regex(/.*/)
  }
}

projects.each { project ->

  jobName = "${getJobName(project)}"
  pipelineName = "${project.name}"
  if (pipelineName != jobName) {
    pipelineName += " | ${jobName}"
  }

  print "Debug: pipelineName: ${pipelineName} | jobName: ${jobName}\n"

  nestedView("${jobName}-views") {

    views {

      buildMonitorView("${jobName}-buildmon") {
        jobs {
          regex(/^${jobName}.*/)
        }

      }

      deliveryPipelineView("${jobName}-delivery-pipeline") {
        pipelineInstances(5)
        showAggregatedPipeline()
        columns(2)
        sorting(Sorting.TITLE)
        updateInterval(2)
//                enableStartBuild()
        enableManualTriggers()
        showAvatars()
        showChangeLog()
        pipelines {
//                    component('Sub System A', 'compile-a')
//                    component('Sub System B', 'compile-b')
          regex(/^(${jobName}.*)/)
        }
      }

      listView("${jobName}-list") {
        description("View for ${pipelineName}")
        jobs {
          regex(/^${jobName}.*/)
        }
        columns defaultListColumns
      }

      dashboardView("${jobName}-dash") {
        jobs {
          regex(/^${jobName}.*/)
        }
        columns defaultListColumns
        topPortlets {
          jenkinsJobsList {
            displayName('acme jobs')
          }
          testStatisticsGrid()
          buildStatistics()
        }
        leftPortlets {
          testStatisticsChart()
        }
        rightPortlets {
          testTrendChart()
        }
        bottomPortlets {
//            iframe {
//                effectiveUrl('http://example.com')
//            }
        }
      }
    }
  }

  scmParameters = {
    git {
      remote {
        url getRepoUrl(project)
      }
      branch(getBranch(project))

//      extensions {
//        cleanBeforeCheckout()
//      }
    }
  }

  pipelineJob(jobName) {
    description "Build ${jobName}"
    disabled(project.disabled)

    authorization {
      project.permissions.each {
        permission(it)
      }
    }

    definition {
      cpsScm {
        scm scmParameters
        scriptPath(getJenkinsfileName(project))
      }
      triggers {
        scm(getPollTrigger(project))
        cron(getCronTrigger(project))
      }
      logRotator(-1, 50, -1, -1)
      concurrentBuild(false)
      compressBuildLog()
      quietPeriod(0)
    }
  }
}
