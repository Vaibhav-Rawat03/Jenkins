def call(String s3Prefix, String lastProjectHashEnv, String lastJavaUtilsHashEnv) {
    script {

echo "DOinggg first....................."
        env.S3_FILE_NAME = sh(
            script: """
                aws s3api list-objects \
                    --bucket jenkinsupdate-test-bucket \
                    --prefix "${s3Prefix}" \
                    --query "Contents | sort_by(@, &LastModified) | [-1]" \
                    --output json | jq -r '.Key' | sed 's/jenkins_backups\\///'
            """,
            returnStdout: true
        ).trim()
        
        env.S3_LAST_MODIFIED = sh(
            script: """
                aws s3api list-objects \
                    --bucket jenkinsupdate-test-bucket \
                    --prefix "${s3Prefix}" \
                    --query "Contents | sort_by(@, &LastModified) | [-1]" \
                    --output json | jq -r '.LastModified'
            """,
            returnStdout: true
        ).trim()
        
echo "DOinggg....................."
        def matches = env.S3_FILE_NAME =~ /.*_([a-f0-9]{40})_([a-f0-9]{40})\.jar$/
        if (matches) {
            env."${lastProjectHashEnv}" = matches[0][1]
            env."${lastJavaUtilsHashEnv}" = matches[0][2]
        } else {
            error("Filename format did not match expected pattern.")
        }
    }
}

def checkForUpdates(String projectDir, String lastProjectHashEnv, String currentProjectHashEnv) {
    script {
        // Fetch the latest commit hashes
        env."${currentProjectHashEnv}" = sh(
            script: """
                cd ${projectDir}
                git rev-parse HEAD
            """,
            returnStdout: true
        ).trim()
        

        // Compare the latest commit hashes with the latest JAR file
        if (env."${lastProjectHashEnv}" == env."${currentProjectHashEnv}") {
            echo "${projectDir} is up to date"
        } else {
            echo "${projectDir} is not up to date"
            echo "Continue to build"
        }

        echo "${projectDir} is going to be built: ${env.UPDATE}"
    }
}

return this
