/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.gui.navigation;

import javafx.stage.FileChooser;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.auth.AccountSettings;
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.gui.GUIFrame;
import net.pistonmaster.serverwrecker.gui.libs.JEnumComboBox;
import net.pistonmaster.serverwrecker.gui.libs.JFXFileHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.libs.SwingTextUtils;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AccountPanel extends NavigationItem implements SettingsDuplex<AccountSettings> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPanel.class);
    private final JTextField nameFormat = new JTextField(AccountSettings.DEFAULT_NAME_FORMAT);
    private final JCheckBox shuffleAccounts = new PresetJCheckBox(AccountSettings.DEFAULT_SHUFFLE_ACCOUNTS);

    @Inject
    public AccountPanel(ServerWrecker serverWrecker, GUIFrame parent) {
        serverWrecker.getSettingsManager().registerDuplex(AccountSettings.class, this);

        setLayout(new GridLayout(2, 1, 10, 10));

        var accountOptionsPanel = new JPanel();
        accountOptionsPanel.setLayout(new GridLayout(2, 1, 10, 10));

        var addAccountPanel = new JPanel();
        addAccountPanel.setLayout(new GridLayout(1, 3, 10, 10));

        addAccountPanel.add(createAccountLoadButton(serverWrecker, parent, AuthType.OFFLINE));
        addAccountPanel.add(createAccountLoadButton(serverWrecker, parent, AuthType.MICROSOFT_JAVA));
        addAccountPanel.add(createAccountLoadButton(serverWrecker, parent, AuthType.MICROSOFT_BEDROCK));
        addAccountPanel.add(createAccountLoadButton(serverWrecker, parent, AuthType.THE_ALTENING));
        addAccountPanel.add(createAccountLoadButton(serverWrecker, parent, AuthType.EASYMC));

        accountOptionsPanel.add(addAccountPanel);

        var accountSettingsPanel = new JPanel();
        accountSettingsPanel.setLayout(new GridLayout(0, 2));

        accountSettingsPanel.add(new JLabel("Shuffle accounts: "));
        accountSettingsPanel.add(shuffleAccounts);

        accountSettingsPanel.add(new JLabel("Name Format: "));
        accountSettingsPanel.add(nameFormat);

        accountOptionsPanel.add(accountSettingsPanel);

        add(accountOptionsPanel);

        var accountListPanel = new JPanel();
        accountListPanel.setLayout(new GridLayout(1, 1));

        var columnNames = new String[]{"Username", "Type", "Enabled"};
        var model = new DefaultTableModel(columnNames, 0) {
            final Class<?>[] columnTypes = new Class<?>[]{String.class, AuthType.class, Boolean.class};

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }
        };

        var accountList = new JTable(model);

        serverWrecker.getAccountRegistry().addLoadHook(() -> {
            model.getDataVector().removeAllElements();

            var registry = serverWrecker.getAccountRegistry();
            var accounts = registry.getAccounts();
            var registrySize = accounts.size();
            var dataVector = new Object[registrySize][];
            for (var i = 0; i < registrySize; i++) {
                var account = accounts.get(i);

                dataVector[i] = new Object[]{
                        account.username(),
                        account.authType(),
                        account.enabled()
                };
            }

            model.setDataVector(dataVector, columnNames);

            accountList.getColumnModel().getColumn(1)
                    .setCellEditor(new DefaultCellEditor(new JEnumComboBox<>(AuthType.class)));

            model.fireTableDataChanged();
        });

        accountList.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName()) && !accountList.isEditing()) {
                List<MinecraftAccount> accounts = new ArrayList<>();

                for (var i = 0; i < accountList.getRowCount(); i++) {
                    var row = new Object[accountList.getColumnCount()];
                    for (var j = 0; j < accountList.getColumnCount(); j++) {
                        row[j] = accountList.getValueAt(i, j);
                    }

                    var username = (String) row[0];
                    var authType = (AuthType) row[1];
                    var enabled = (boolean) row[2];

                    var account = serverWrecker.getAccountRegistry().getAccount(username, authType);

                    accounts.add(new MinecraftAccount(authType, username, account.accountData(), enabled));
                }

                serverWrecker.getAccountRegistry().setAccounts(accounts);
            }
        });

        var scrollPane = new JScrollPane(accountList);

        accountListPanel.add(scrollPane);

        add(accountListPanel);
    }

    private JButton createAccountLoadButton(ServerWrecker serverWrecker, GUIFrame parent, AuthType type) {
        var loadText = SwingTextUtils.htmlCenterText(String.format("Load %s accounts", type));
        var typeText = String.format("%s list file", type);
        var button = new JButton(loadText);

        var chooser = new FileChooser();
        chooser.setInitialDirectory(Path.of(System.getProperty("user.dir")).toFile());
        chooser.setTitle(loadText);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(typeText, "*.txt"));

        button.addActionListener(new LoadAccountsListener(serverWrecker, parent, chooser, type));
        return button;
    }

    @Override
    public String getNavigationName() {
        return "Accounts";
    }

    @Override
    public String getNavigationId() {
        return "account-menu";
    }

    @Override
    public void onSettingsChange(AccountSettings settings) {
        nameFormat.setText(settings.nameFormat());
        shuffleAccounts.setSelected(settings.shuffleAccounts());
    }

    @Override
    public AccountSettings collectSettings() {
        return new AccountSettings(
                nameFormat.getText(),
                shuffleAccounts.isSelected()
        );
    }

    private record LoadAccountsListener(ServerWrecker serverWrecker, GUIFrame frame,
                                        FileChooser chooser, AuthType authType) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            var accountFile = JFXFileHelper.showOpenDialog(chooser);
            if (accountFile == null) {
                return;
            }

            LOGGER.info("Opening: {}", accountFile.getFileName());
            serverWrecker.getThreadPool().submit(() -> {
                try {
                    serverWrecker.getAccountRegistry().loadFromFile(accountFile, authType);
                } catch (Throwable e) {
                    LOGGER.error("Failed to load accounts!", e);
                }
            });
        }
    }
}
