package org.picketlink.internal;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.Entity;

import org.picketlink.IdentityConfigurationEvent;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.FileIdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.jpa.annotations.IDMEntity;

/**
 * Automatic configuration builder for JPAIdentityStore
 * 
 * @author Shane Bryzak
 *
 */
@ApplicationScoped
public class IdentityStoreAutoConfiguration implements Extension {

    private JPAIdentityStoreConfiguration jpaConfig = new JPAIdentityStoreConfiguration();

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event,
            final BeanManager beanManager) {

        if (event.getAnnotatedType().isAnnotationPresent(Entity.class)) {
            AnnotatedType<X> type = event.getAnnotatedType();

            if (type.isAnnotationPresent(IDMEntity.class)) {
                IDMEntity a = type.getAnnotation(IDMEntity.class);

                switch(a.value()) { 
                    case IDENTITY_TYPE: 
                        jpaConfig.setIdentityClass(type.getJavaClass());
                        break;
                    case IDENTITY_CREDENTIAL:
                        jpaConfig.setCredentialClass(type.getJavaClass());
                        break;
                    case CREDENTIAL_ATTRIBUTE:
                        jpaConfig.setCredentialAttributeClass(type.getJavaClass());
                    case IDENTITY_ATTRIBUTE:
                        jpaConfig.setAttributeClass(type.getJavaClass());
                        break;
                    case RELATIONSHIP:
                        jpaConfig.setRelationshipClass(type.getJavaClass());
                        break;
                    case RELATIONSHIP_IDENTITY:
                        jpaConfig.setRelationshipIdentityClass(type.getJavaClass());
                        break;
                    case RELATIONSHIP_ATTRIBUTE:
                        jpaConfig.setRelationshipAttributeClass(type.getJavaClass());
                        break;
                    case PARTITION:
                        jpaConfig.setPartitionClass(type.getJavaClass());
                        break;
                }
            }
        }
    }

    public void observesConfigurationEvent(@Observes IdentityConfigurationEvent event) {
        if (jpaConfig.isConfigured()) {
            FeatureSet.addFeatureSupport(jpaConfig.getFeatureSet());
            FeatureSet.addRelationshipSupport(jpaConfig.getFeatureSet());
            jpaConfig.getFeatureSet().setSupportsCustomRelationships(true);
            jpaConfig.getFeatureSet().setSupportsMultiRealm(true);
            event.getConfig().addStoreConfiguration(jpaConfig);
        } else {
            FileIdentityStoreConfiguration config = new FileIdentityStoreConfiguration();
            FeatureSet.addFeatureSupport(config.getFeatureSet());
            FeatureSet.addRelationshipSupport(config.getFeatureSet());
            config.getFeatureSet().setSupportsCustomRelationships(true);
            config.getFeatureSet().setSupportsMultiRealm(true);
            event.getConfig().addStoreConfiguration(config);
        }
    }
}
