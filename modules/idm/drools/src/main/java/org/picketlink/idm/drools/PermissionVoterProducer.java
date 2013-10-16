package org.picketlink.idm.drools;

import java.io.InputStream;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.KieResources;
import org.kie.api.runtime.KieContainer;
import org.picketlink.idm.permission.spi.PermissionVoter;

/**
 * Produces a DroolsPermissionVoter if a security rules drl file is found.
 *
 * @author Shane Bryzak
 *
 */
public class PermissionVoterProducer {

    private static final String SECURITY_RULES = "/WEB-INF/classes/security/security-rules.drl";

    /* THIS DOESN'T WORK YET - SEE DROOLS-299
    @Inject
    @KSession("ksession1")
    KieSession kSession;
*/

    private DroolsPermissionVoter voter;

    /**
     * This is all a nasty hack for now until Drools 6 supports injection of rules based on
     * a configuration defined within a war file - see DROOLS-299 in JIRA
     */
    @Inject
    public void init(@Web ServletContext context) {
        KieServices kServices = KieServices.Factory.get();

        KieResources kieResources = kServices.getResources();
        KieFileSystem kieFileSystem = kServices.newKieFileSystem();
        InputStream in = getClass().getResourceAsStream(SECURITY_RULES);
        String path = "src/main/resources/optaplanner-kie-namespace/" + SECURITY_RULES;
        kieFileSystem.write(path, kieResources.newInputStreamResource(in, "UTF-8"));

        KieBuilder kieBuilder = kServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            StringBuilder sb = new StringBuilder();
            for (Message msg : results.getMessages(Message.Level.ERROR)) {
                sb.append(msg.getText());
            }
            throw new RuntimeException("Error parsing security rules: " + sb.toString());
        }

        KieContainer kieContainer = kServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());


        //KieContainer kContainer = kServices.getKieClasspathContainer();
        //kContainer = KieServices.Factory.get().getKieClasspathContainer();

        KieBase kBase = kieContainer.getKieBase("security");
        //KieSession ks = kc.newKieSession("ksession1");

        //KieBaseConfiguration kieBaseConfiguration = kieServices.newKieBaseConfiguration();
        //kBase = kieContainer.newKieBase(kieBaseConfiguration);

        voter = new DroolsPermissionVoter(kBase);
    }

    @Produces
    public PermissionVoter createPermissionVoter() {
        return voter;
    }
}
