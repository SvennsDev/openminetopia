package nl.openminetopia.modules.banking;

import com.craftmend.storm.api.enums.Where;
import lombok.Getter;
import lombok.SneakyThrows;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.banking.commands.BankingCreateCommand;
import nl.openminetopia.modules.banking.commands.BankingOpenCommand;
import nl.openminetopia.modules.banking.commands.BankingUsersCommand;
import nl.openminetopia.modules.banking.enums.AccountType;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.data.storm.models.BankAccountModel;
import nl.openminetopia.modules.data.storm.models.BankPermissionModel;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Todo:
 * - Commands (delete, removeuser, setbalance, freeze, pin)
 * - Menu's (banking inventory)
 * - Debitcards
 */

@Getter
public class BankingModule extends Module {

    private List<BankAccountModel> bankAccountModels = new ArrayList<>();

    @Override
    public void enable() {
        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            OpenMinetopia.getInstance().getLogger().info("Loading bank accounts..");
            loadAccounts().whenComplete((accounts, accountThrowable) -> {
                if (accountThrowable != null) {
                    OpenMinetopia.getInstance().getLogger().severe("Something went wrong while trying to load all bank accounts: " + accountThrowable.getMessage());
                    return;
                }

                bankAccountModels = accounts;
                OpenMinetopia.getInstance().getLogger().info("Loaded a total of " + bankAccountModels.size() + " accounts.");

                loadPermissions().whenComplete((amount, permissionThrowable) -> {
                    if (permissionThrowable != null) {
                        OpenMinetopia.getInstance().getLogger().severe("Something went wrong while trying to bank permission: " + permissionThrowable.getMessage());
                        return;
                    }

                    OpenMinetopia.getInstance().getLogger().info("Applied " + amount + " permissions.");
                });

            });
        }, 20L);

        registerCommand(new BankingCreateCommand());
        registerCommand(new BankingUsersCommand());
        registerCommand(new BankingOpenCommand());
    }

    @Override
    public void disable() {
        // todo: save all accounts.
    }

    public List<BankAccountModel> getAccountsFromPlayer(UUID uuid) {
        return bankAccountModels.stream().filter(account -> account.getUsers().containsKey(uuid)).collect(Collectors.toList());
    }

    public BankAccountModel getAccountByName(String name) {
        return bankAccountModels.stream().filter(account -> account.getName().equals(name)).findAny().orElse(null);
    }

    public BankAccountModel getAccountById(UUID uuid) {
        return bankAccountModels.stream().filter(account -> account.getUniqueId().equals(uuid)).findAny().orElse(null);
    }

    public CompletableFuture<List<BankAccountModel>> loadAccounts() {
        CompletableFuture<List<BankAccountModel>> completableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Collection<BankAccountModel>> modelFuture = StormDatabase.getInstance().getStorm().buildQuery(BankAccountModel.class)
                    .where("type", Where.NOT_EQUAL, AccountType.PRIVATE.toString())
                    .execute();

            modelFuture.whenComplete((models, throwable) -> {
                if (throwable != null) {
                    completableFuture.completeExceptionally(throwable);
                    return;
                }

                completableFuture.complete(new ArrayList<>(models));
            });

        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    public CompletableFuture<Integer> loadPermissions() {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Collection<BankPermissionModel>> permissionsFuture = StormDatabase.getInstance().getStorm().buildQuery(BankPermissionModel.class)
                    .execute();

            permissionsFuture.whenComplete((permissions, throwable) -> {
                for (BankPermissionModel permission : permissions) {
                    BankAccountModel accountModel = getAccountById(permission.getAccount());
                    if(accountModel == null) {
                        // todo: remove from db, account isn't valid?
                        continue;
                    }
                    accountModel.getUsers().put(permission.getUuid(), permission.getPermission());
                    OpenMinetopia.getInstance().getLogger().info(" -> " + permission.getUuid().toString() + ": " + permission.getPermission().toString());
                }

                completableFuture.complete(permissions.size());
            });
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    @SneakyThrows
    public CompletableFuture<BankAccountModel> getAccountModel(UUID accountId) {
        CompletableFuture<BankAccountModel> accountModelFuture = new CompletableFuture<>();

        CompletableFuture<Collection<BankAccountModel>> collectionFuture = StormDatabase.getInstance().getStorm()
                .buildQuery(BankAccountModel.class)
                .where("uuid", Where.EQUAL, accountId.toString())
                .execute();

        collectionFuture.whenComplete((collection, throwable) -> {
            if(throwable != null) {
                accountModelFuture.completeExceptionally(throwable);
                return;
            }

            BankAccountModel bankAccountModel = collection.stream().findFirst().orElse(null);
            accountModelFuture.complete(bankAccountModel);
        });

        return accountModelFuture;
    }

    public int createAccount(BankAccountModel accountModel) throws SQLException {
        this.getBankAccountModels().add(accountModel);
        return StormDatabase.getInstance().getStorm().save(accountModel);
    }

    public int createPermissions(BankPermissionModel permissionModel) throws SQLException {
        BankAccountModel accountModel = getAccountById(permissionModel.getAccount());
        accountModel.getUsers().put(permissionModel.getUuid(), permissionModel.getPermission());
        return StormDatabase.getInstance().getStorm().save(permissionModel);
    }

}
