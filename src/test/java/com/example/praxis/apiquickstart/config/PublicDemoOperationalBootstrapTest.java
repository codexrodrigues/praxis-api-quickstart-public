package com.example.praxis.apiquickstart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PublicDemoOperationalBootstrapTest {

    @Test
    void rewritesOnlyTheFourDocumentedDumpRolesToAQuotedMigrationIdentity() {
        assertThat(PublicDemoOperationalBootstrap.rewriteRole(
                "ALTER TABLE public.funcionarios OWNER TO praxis_demo_owner;", "\"render-user\""))
                .isEqualTo("ALTER TABLE public.funcionarios OWNER TO \"render-user\";");
        assertThat(PublicDemoOperationalBootstrap.rewriteRole(
                "GRANT SELECT ON TABLE public.funcionarios TO praxis_service_user;", "\"render-user\""))
                .isEqualTo("GRANT SELECT ON TABLE public.funcionarios TO \"render-user\";");
        assertThat(PublicDemoOperationalBootstrap.rewriteRole(
                "ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA public GRANT ALL ON TABLES TO praxis_demo_superuser WITH GRANT OPTION;",
                "\"render-user\""))
                .isEqualTo("ALTER DEFAULT PRIVILEGES FOR ROLE \"render-user\" IN SCHEMA public GRANT ALL ON TABLES TO \"render-user\" WITH GRANT OPTION;");
    }

    @Test
    void rejectsAnOwnerOrGranteeNotDeclaredByTheVersionedDumpContract() {
        assertThatThrownBy(() -> PublicDemoOperationalBootstrap.rewriteRole(
                "ALTER TABLE public.funcionarios OWNER TO arbitrary_runtime;", "\"migration\""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Public-demo dump references an ungoverned role");
        assertThatThrownBy(() -> PublicDemoOperationalBootstrap.rewriteRole(
                "GRANT SELECT ON TABLE public.funcionarios TO arbitrary_runtime;", "\"migration\""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Public-demo dump references an ungoverned role");
    }

    @Test
    void skipsOnlyTheKnownPostgres17SessionSettingOnOlderPostgres() {
        assertThat(PublicDemoOperationalBootstrap.isUnsupportedPortableSessionSetting(
                "SET transaction_timeout = 0;"))
                .isTrue();
        assertThat(PublicDemoOperationalBootstrap.isUnsupportedPortableSessionSetting(
                "SET statement_timeout = 0;"))
                .isFalse();
        assertThat(PublicDemoOperationalBootstrap.isUnsupportedPortableSessionSetting(
                "SET transaction_timeout = '30s';"))
                .isFalse();
    }
}
