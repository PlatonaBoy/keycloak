import {
  Button,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
  SearchInput,
  Tooltip,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, type Path } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { toAuthentication } from "../../authentication/routes/Authentication";
import { toClients } from "../../clients/routes/Clients";
import { toClient } from "../../clients/routes/Client";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { toGroups } from "../../groups/routes/Groups";
import { toIdentityProviders } from "../../identity-providers/routes/IdentityProviders";
import { toRealmSettings } from "../../realm-settings/routes/RealmSettings";
import { toUser } from "../../user/routes/User";
import { toUsers } from "../../user/routes/Users";
import { getProtocolName } from "../../clients/utils";

type SearchHit = {
  id: string;
  title: string;
  description: string;
  to: Partial<Path>;
};

const PAGE_LIMIT = 6;

export const AdminSearch = () => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<SearchHit[]>([]);
  const [searching, setSearching] = useState(false);

  const canQueryUsers = hasAccess("query-users");
  const canQueryClients = hasAccess("query-clients");
  const canQueryGroups = hasAccess("query-groups");
  const canViewIdps = hasAccess("view-identity-providers");
  const canViewRealm = hasAccess("view-realm");

  const pages = useMemo<SearchHit[]>(
    () =>
      [
        {
          id: "page-home",
          title: t("home"),
          description: t("adminSearchHomeHelp"),
          to: toDashboard({ realm }),
          visible: true,
        },
        {
          id: "page-users",
          title: t("titleUsers"),
          description: t("adminSearchUsersHelp"),
          to: toUsers({ realm, tab: "list" }),
          visible: canQueryUsers,
        },
        {
          id: "page-clients",
          title: t("clientList"),
          description: t("adminSearchClientsHelp"),
          to: toClients({ realm, tab: "list" }),
          visible: canQueryClients,
        },
        {
          id: "page-groups",
          title: t("groups"),
          description: t("adminSearchGroupsHelp"),
          to: toGroups({ realm }),
          visible: canQueryGroups,
        },
        {
          id: "page-idps",
          title: t("identityProviders"),
          description: t("adminSearchIdpHelp"),
          to: toIdentityProviders({ realm }),
          visible: canViewIdps,
        },
        {
          id: "page-auth",
          title: t("authentication"),
          description: t("adminSearchAuthHelp"),
          to: toAuthentication({ realm }),
          visible: canViewRealm || canViewIdps || canQueryClients,
        },
        {
          id: "page-settings",
          title: t("realmSettings"),
          description: t("adminSearchSettingsHelp"),
          to: toRealmSettings({ realm }),
          visible: canViewRealm,
        },
      ].filter((page) => page.visible),
    [
      t,
      realm,
      canQueryUsers,
      canQueryClients,
      canQueryGroups,
      canViewIdps,
      canViewRealm,
    ],
  );

  const close = () => {
    setOpen(false);
    setQuery("");
    setHits([]);
  };

  const openSearch = useCallback(() => {
    setOpen(true);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen((current) => !current);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  useEffect(() => {
    const term = query.trim();
    if (!open) {
      return;
    }
    if (!term) {
      setHits([]);
      setSearching(false);
      return;
    }

    let cancelled = false;
    setSearching(true);
    const handle = window.setTimeout(async () => {
      try {
        const [users, clients] = await Promise.allSettled([
          canQueryUsers
            ? adminClient.users.find({
                search: term,
                max: PAGE_LIMIT,
                briefRepresentation: true,
              })
            : Promise.resolve([]),
          canQueryClients
            ? adminClient.clients.find({
                clientId: term,
                search: true,
                max: PAGE_LIMIT,
              })
            : Promise.resolve([]),
        ]);

        if (cancelled) {
          return;
        }

        const userHits: SearchHit[] =
          users.status === "fulfilled"
            ? users.value.map((user) => ({
                id: `user-${user.id}`,
                title: user.username ?? user.id!,
                description: [user.email, user.firstName, user.lastName]
                  .filter(Boolean)
                  .join(" · "),
                to: toUser({ realm, id: user.id!, tab: "settings" }),
              }))
            : [];

        const clientHits: SearchHit[] =
          clients.status === "fulfilled"
            ? clients.value.map((client) => ({
                id: `client-${client.id}`,
                title: client.clientId ?? client.id!,
                description: getProtocolName(
                  t,
                  client.protocol ?? "openid-connect",
                ),
                to: toClient({ realm, clientId: client.id!, tab: "settings" }),
              }))
            : [];

        setHits([...userHits, ...clientHits]);
      } finally {
        if (!cancelled) {
          setSearching(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(handle);
    };
  }, [adminClient, canQueryClients, canQueryUsers, open, query, realm, t]);

  const filteredPages = pages.filter((page) => {
    const term = query.trim().toLowerCase();
    if (!term) {
      return true;
    }
    return (
      page.title.toLowerCase().includes(term) ||
      page.description.toLowerCase().includes(term)
    );
  });

  const go = (to: SearchHit["to"]) => {
    close();
    void navigate(to);
  };

  const shortcut = /Mac|iPhone|iPad/i.test(navigator.userAgent)
    ? "⌘K"
    : "Ctrl+K";

  return (
    <>
      <Tooltip content={t("adminSearchTooltip", { shortcut })}>
        <Button
          variant="plain"
          icon={<SearchIcon />}
          aria-label={t("adminSearch")}
          data-testid="admin-search-button"
          onClick={openSearch}
        />
      </Tooltip>
      <Modal
        isOpen={open}
        onClose={close}
        variant={ModalVariant.medium}
        title={t("adminSearch")}
        data-testid="admin-search-modal"
      >
        <SearchInput
          aria-label={t("adminSearch")}
          placeholder={t("adminSearchPlaceholder")}
          value={query}
          autoFocus
          onChange={(_event, value) => setQuery(value)}
          onClear={() => setQuery("")}
          onKeyDown={(event) => {
            if (event.key !== "Enter") {
              return;
            }
            const first = hits.at(0) ?? filteredPages.at(0);
            if (first === undefined) {
              return;
            }
            event.preventDefault();
            go(first.to);
          }}
          data-testid="admin-search-input"
        />
        <DataList
          aria-label={t("adminSearch")}
          className="pf-v5-u-mt-md"
          isCompact
        >
          {hits.map((hit) => (
            <DataListItem
              key={hit.id}
              id={hit.id}
              onClick={() => go(hit.to)}
              data-testid={`admin-search-hit-${hit.id}`}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="title" width={2}>
                      {hit.title}
                    </DataListCell>,
                    <DataListCell key="description" width={3}>
                      {hit.description || t("adminSearchResult")}
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
          ))}
          {filteredPages.map((page) => (
            <DataListItem
              key={page.id}
              id={page.id}
              onClick={() => go(page.to)}
              data-testid={`admin-search-page-${page.id}`}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="title" width={2}>
                      {page.title}
                    </DataListCell>,
                    <DataListCell key="description" width={3}>
                      {page.description}
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
          ))}
          {searching && (
            <DataListItem id="searching">
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="searching">
                      {t("searching")}
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
          )}
        </DataList>
      </Modal>
    </>
  );
};
