import { useEnvironment, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Card,
  CardBody,
  CardFooter,
  CardTitle,
  ClipboardCopy,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Gallery,
  GalleryItem,
  Grid,
  GridItem,
  Label,
  PageSection,
  Title,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, type Path } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { toAuthentication } from "../authentication/routes/Authentication";
import { toAddClient } from "../clients/routes/AddClient";
import { toClients } from "../clients/routes/Clients";
import { toGroups } from "../groups/routes/Groups";
import { toIdentityProviders } from "../identity-providers/routes/IdentityProviders";
import { toRealmSettings } from "../realm-settings/routes/RealmSettings";
import { toAddUser } from "../user/routes/AddUser";
import { toUsers } from "../user/routes/Users";
import { resolveDisplayName } from "../util";
import { buildRealmEndpoints } from "./realm-endpoints";

type OverviewStats = {
  userCount?: number;
  clientCount?: number;
  idpCount?: number;
};

type ShortcutCard = {
  id: string;
  titleKey: string;
  descriptionKey: string;
  to: Partial<Path>;
  createTo?: Partial<Path>;
  createKey?: string;
  count?: number;
  visible: boolean;
};

export const RealmOverview = ({ showTitle = true }: { showTitle?: boolean }) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { environment } = useEnvironment();
  const { realm, realmRepresentation } = useRealm();
  const { hasAccess } = useAccess();
  const [stats, setStats] = useState<OverviewStats>({});

  const canQueryUsers = hasAccess("query-users");
  const canQueryClients = hasAccess("query-clients");
  const canViewIdps = hasAccess("view-identity-providers");
  const canViewRealm = hasAccess("view-realm");
  const canQueryGroups = hasAccess("query-groups");
  const canManageUsers = hasAccess("manage-users");
  const canManageClients = hasAccess("manage-clients");

  useFetch(
    async () => {
      const [users, clients, idps] = await Promise.allSettled([
        canQueryUsers ? adminClient.users.count() : Promise.resolve(undefined),
        canQueryClients
          ? adminClient.clients.find({ max: 101, briefRepresentation: true })
          : Promise.resolve(undefined),
        canViewIdps
          ? adminClient.identityProviders.find({ max: 101 })
          : Promise.resolve(undefined),
      ]);

      return {
        userCount: users.status === "fulfilled" ? users.value : undefined,
        clientCount:
          clients.status === "fulfilled" ? clients.value?.length : undefined,
        idpCount: idps.status === "fulfilled" ? idps.value?.length : undefined,
      } satisfies OverviewStats;
    },
    setStats,
    [realm, canQueryUsers, canQueryClients, canViewIdps],
  );

  const displayName = resolveDisplayName(
    t,
    realmRepresentation.displayName,
    realm,
  );
  const endpoints = buildRealmEndpoints(environment.serverBaseUrl, realm);

  const formatCount = (count?: number) => {
    if (count === undefined) {
      return undefined;
    }
    return count > 100 ? "100+" : String(count);
  };

  const shortcuts: ShortcutCard[] = [
    {
      id: "users",
      titleKey: "titleUsers",
      descriptionKey: "realmOverviewUsersHelp",
      to: toUsers({ realm, tab: "list" }),
      createTo: toAddUser({ realm }),
      createKey: "addUser",
      count: stats.userCount,
      visible: canQueryUsers,
    },
    {
      id: "clients",
      titleKey: "clientList",
      descriptionKey: "realmOverviewClientsHelp",
      to: toClients({ realm, tab: "list" }),
      createTo: toAddClient({ realm }),
      createKey: "createClient",
      count: stats.clientCount,
      visible: canQueryClients,
    },
    {
      id: "groups",
      titleKey: "groups",
      descriptionKey: "realmOverviewGroupsHelp",
      to: toGroups({ realm }),
      visible: canQueryGroups,
    },
    {
      id: "identity-providers",
      titleKey: "identityProviders",
      descriptionKey: "realmOverviewIdpHelp",
      to: toIdentityProviders({ realm }),
      count: stats.idpCount,
      visible: canViewIdps,
    },
    {
      id: "authentication",
      titleKey: "authentication",
      descriptionKey: "realmOverviewAuthHelp",
      to: toAuthentication({ realm }),
      visible: canViewRealm || canViewIdps || canQueryClients,
    },
    {
      id: "realm-settings",
      titleKey: "realmSettings",
      descriptionKey: "realmOverviewSettingsHelp",
      to: toRealmSettings({ realm }),
      visible: canViewRealm,
    },
  ].filter((card) => card.visible);

  return (
    <PageSection variant="light" data-testid="realm-overview">
      {showTitle && (
        <>
          <Title
            headingLevel="h1"
            size="2xl"
            className="pf-v5-u-mb-sm"
            data-testid="welcomeTitle"
          >
            {t("welcomeTo", { realmDisplayInfo: displayName })}
          </Title>
          <p className="pf-v5-u-mb-lg" data-testid="realm-overview-intro">
            {t("realmOverviewIntro")}{" "}
            <Label color={realmRepresentation.enabled ? "green" : "red"}>
              {realmRepresentation.enabled ? t("enabled") : t("disabled")}
            </Label>
          </p>
        </>
      )}

      {shortcuts.length > 0 && (
        <Gallery hasGutter className="pf-v5-u-mb-lg">
          {shortcuts.map((card) => (
            <GalleryItem key={card.id}>
              <Card isCompact className="keycloak__dashboard_card">
                <CardTitle>
                  <Link
                    to={card.to}
                    data-testid={`realm-overview-${card.id}`}
                    className="pf-v5-u-font-size-lg"
                  >
                    {t(card.titleKey)}
                  </Link>
                  {formatCount(card.count) !== undefined && (
                    <Label className="pf-v5-u-ml-sm" isCompact>
                      {formatCount(card.count)}
                    </Label>
                  )}
                </CardTitle>
                <CardBody>{t(card.descriptionKey)}</CardBody>
                {card.createTo &&
                  ((card.id === "users" && canManageUsers) ||
                    (card.id === "clients" && canManageClients)) && (
                    <CardFooter>
                      <Link
                        to={card.createTo}
                        data-testid={`realm-overview-create-${card.id}`}
                      >
                        {t(card.createKey!)}
                      </Link>
                    </CardFooter>
                  )}
              </Card>
            </GalleryItem>
          ))}
        </Gallery>
      )}

      <Grid hasGutter>
        <GridItem md={6} sm={12}>
          <Card className="keycloak__dashboard_card">
            <CardTitle>{t("realmOverviewSnapshot")}</CardTitle>
            <CardBody>
              <DescriptionList isHorizontal isCompact>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("realm")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {realm}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("userRegistration")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {realmRepresentation.registrationAllowed
                      ? t("enabled")
                      : t("disabled")}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("loginWithEmailAllowed")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {realmRepresentation.loginWithEmailAllowed
                      ? t("enabled")
                      : t("disabled")}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("requireSsl")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {realmRepresentation.sslRequired
                      ? t(`sslType.${realmRepresentation.sslRequired}`)
                      : "—"}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("bruteForceDetection")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {realmRepresentation.bruteForceProtected
                      ? t("enabled")
                      : t("disabled")}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </DescriptionList>
            </CardBody>
          </Card>
        </GridItem>
        <GridItem md={6} sm={12}>
          <Card className="keycloak__dashboard_card">
            <CardTitle>{t("realmOverviewEndpoints")}</CardTitle>
            <CardBody>
              <DescriptionList isCompact>
                {endpoints.map((endpoint) => (
                  <DescriptionListGroup key={endpoint.id}>
                    <DescriptionListTerm>
                      {t(endpoint.labelKey)}
                    </DescriptionListTerm>
                    <DescriptionListDescription>
                      <ClipboardCopy
                        isReadOnly
                        hoverTip={t("copyToClipboard")}
                        clickTip={t("copySuccess")}
                        data-testid={`realm-endpoint-${endpoint.id}`}
                      >
                        {endpoint.url}
                      </ClipboardCopy>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                ))}
              </DescriptionList>
            </CardBody>
          </Card>
        </GridItem>
      </Grid>
    </PageSection>
  );
};
