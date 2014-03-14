package org.jenkinsci.test.acceptance.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.jenkinsci.test.acceptance.guice.TestScope;
import org.jenkinsci.test.acceptance.resolver.JenkinsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Creates {@link JenkinsController} that launches Jenkins on a {@link Machine}.
 *
 * @author Vivek Pandey
 */
@TestScope
public class JenkinsProvider implements Provider<JenkinsController> {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsProvider.class);

    private final Machine machine;


    private final String jenkinsHome;

    private final JenkinsController jenkinsController;

    private final JenkinsResolver jenkinsResolver;

    @Inject
    public JenkinsProvider(Machine machine, JenkinsResolver jenkinsResolver, @Named("privateKeyFile") File privateKeyFile) {
        this.machine = machine;
        this.jenkinsResolver = jenkinsResolver;
        logger.info("New Jenkins Provider created");
        try{
            //install jenkins
            String jenkinsWar = jenkinsResolver.materialize(machine);
            this.jenkinsHome = machine.dir()+"/"+newJenkinsHome();
            try {
                Ssh ssh = machine.connect();
                ssh.executeRemoteCommand("mkdir -p " + jenkinsHome + "/plugins");

                File formPathElement = JenkinsController.downloadPathElement();

                //copy form-path-element
                ssh.copyTo(formPathElement.getAbsolutePath(), "path-element.hpi", "./"+jenkinsHome+"/plugins/");

                // TODO: should support different controllers that launches Jenkins differently
                this.jenkinsController = new RemoteJenkinsController(machine, jenkinsHome,jenkinsWar,privateKeyFile);
            } catch (IOException e) {
                throw new AssertionError("Failed to copy form-path-element.hpi",e);
            }

        }catch(Exception e){
            try {
                machine.close();
            } catch (IOException e1) {
                throw new AssertionError(e);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public JenkinsController get() {
        logger.info("New RemoteJenkinsController created");
        try {
            jenkinsController.start();
        } catch (IOException e) {
            throw new AssertionError("Failed to start Jenkins: "+e.getMessage(),e);
        }
        return jenkinsController;
    }

    private String newJenkinsHome(){
        return String.format("jenkins_home_%s", JcloudsMachine.newDirSuffix());
    }

}
