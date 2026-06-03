-- Executado no primeiro boot pelo entrypoint do Postgres
-- Garante as roles necessarias usadas no dump
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'praxis_demo_owner') THEN
        CREATE ROLE praxis_demo_owner;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'praxis_service_user') THEN
    CREATE ROLE praxis_service_user;
  END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'praxis_demo_superuser') THEN
        CREATE ROLE praxis_demo_superuser;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cloud_admin') THEN
    CREATE ROLE cloud_admin;
  END IF;
END
$$;
