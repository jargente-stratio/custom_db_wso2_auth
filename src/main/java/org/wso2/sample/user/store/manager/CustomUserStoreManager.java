package org.wso2.sample.user.store.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.utils.Secret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;


/**
 * Sample User Store Manager Class
 * <p/>
 * This is a sample user store manage for a user table which contains columns -
 * customer_id, customer_name and password
 * <p/>
 * This has been extended the JDBCUserStoreManager class  which is shipped with carbon.user.core
 * bundle and override some methods.
 * <p/>
 * JDBCUserStoreManager can not be used for a user table with contains two columns. Therefore these
 * override method just ensure that reading is done according to the custom schema.
 * Therefore most of the override methods are same as the methods in JDBCUserStoreManager class.
 * <p/>
 * Some functionality has been limited this user table such as tenant aware, salted password
 * value ,creating time of user and etc.
 * <p/>
 * This class only a sample demonstration of writing a custom user store manager. Also anyone can
 * write their own implementation by extending AbstractUserStoreManager or implementing UserStoreManager
 */
public class CustomUserStoreManager extends JDBCUserStoreManager {


    private static Log log = LogFactory.getLog(CustomUserStoreManager.class);

    public CustomUserStoreManager() {
    }

    public CustomUserStoreManager(org.wso2.carbon.user.api.RealmConfiguration realmConfig,
                                  Map<String, Object> properties,
                                  ClaimManager claimManager,
                                  ProfileConfigurationManager profileManager,
                                  UserRealm realm, Integer tenantId)
            throws UserStoreException {
        super(realmConfig, properties, claimManager, profileManager, realm, tenantId, false);
    }

    @Override
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {

        if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
            log.error("Anonymous user trying to login");
            return false;
        }

        Connection dbConnection = null;
        ResultSet rs = null;
        PreparedStatement prepStmt = null;
        String sqlstmt = null;

        String password = String.valueOf(((Secret)credential).getChars());
        boolean isAuthed = false;

        try {
            dbConnection = getDBConnection();
            dbConnection.setAutoCommit(false);
            //paring the SELECT_USER_SQL from user_mgt.xml
            sqlstmt = realmConfig.getUserStoreProperty(JDBCRealmConstants.SELECT_USER);

            if (log.isDebugEnabled()) {
                log.debug(sqlstmt);
            }

            prepStmt = dbConnection.prepareStatement(sqlstmt);
            prepStmt.setString(1, userName);

            rs = prepStmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString(1);
                if ((storedPassword != null) && (storedPassword.trim().equals(password))) {
                    isAuthed = true;
                }

            }
        } catch (SQLException e) {
            throw new UserStoreException("Authentication Failure. Using sql :" + sqlstmt);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }

        if (log.isDebugEnabled()) {
            log.debug("User " + userName + " login attempt. Login success :: " + isAuthed);
        }

        return isAuthed;

    }

    @Override
    public Date getPasswordExpirationTime(String userName) throws UserStoreException {
        return null;
    }

    protected boolean isValueExisting(String sqlStmt, Connection dbConnection, Object... params)
            throws UserStoreException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        boolean isExisting = false;
        boolean doClose = false;
        try {
            if (dbConnection == null) {
                dbConnection = getDBConnection();
                doClose = true; //because we created it
            }
            if (DatabaseUtil.getStringValuesFromDatabase(dbConnection, sqlStmt, params).length > 0) {
                isExisting = true;
            }
            return isExisting;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            log.error("Using sql : " + sqlStmt);
            throw new UserStoreException(e.getMessage(), e);
        } finally {
            if (doClose) {
                DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
            }
        }
    }

    public String[] getUserListFromProperties(String property, String value, String profileName)
            throws UserStoreException {
        return new String[0];
    }


    /*@Override
    public Map<String, String> doGetUserClaimValues(String userName, String[] claims,
                                                    String domainName) throws UserStoreException {
        return new HashMap<String, String>();
    }*/

    /*@Override
    public String doGetUserClaimValue(String userName, String claim, String profileName)
            throws UserStoreException {
        return null;
    }*/

    @Override
    public boolean isReadOnly() throws UserStoreException {
        return true;
    }

    @Override
    public void doAddUser(String userName, Object credential, String[] roleList,
                          Map<String, String> claims, String profileName,
                          boolean requirePasswordChange) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    public void doAddRole(String roleName, String[] userList, org.wso2.carbon.user.api.Permission[] permissions)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doDeleteRole(String roleName) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doDeleteUser(String userName) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public boolean isBulkImportSupported() {
        return false;
    }

    @Override
    public void doUpdateRoleName(String roleName, String newRoleName) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doSetUserClaimValue(String userName, String claimURI, String claimValue,
                                    String profileName) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doSetUserClaimValues(String userName, Map<String, String> claims,
                                     String profileName) throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doDeleteUserClaimValue(String userName, String claimURI, String profileName)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doDeleteUserClaimValues(String userName, String[] claims, String profileName)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doUpdateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    @Override
    public void doUpdateCredentialByAdmin(String userName, Object newCredential)
            throws UserStoreException {
        throw new UserStoreException(
                "User store is operating in read only mode. Cannot write into the user store.");
    }

    public String[] getExternalRoleListOfUser(String userName) throws UserStoreException {
        /*informix user store manager is supposed to be read only and users in the custom user store
          users in the custom user store are only assigned to internal roles. Therefore this method
          returns an empty string.
         */

        return new String[0];
    }

    @Override
    public String[] doGetRoleNames(String filter, int maxItemLimit) throws UserStoreException {
        return new String[0];
    }

    @Override
    public boolean doCheckExistingRole(String roleName) throws UserStoreException {

        return false;
    }

    @Override
    public boolean doCheckExistingUser(String userName) throws UserStoreException {

        return true;
    }

    @Override
    public org.wso2.carbon.user.api.Properties getDefaultUserStoreProperties(){
        Properties properties = new Properties();
        properties.setMandatoryProperties(CustomUserStoreConstants.CUSTOM_UM_MANDATORY_PROPERTIES.toArray
                (new Property[CustomUserStoreConstants.CUSTOM_UM_MANDATORY_PROPERTIES.size()]));
        properties.setOptionalProperties(CustomUserStoreConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.toArray
                (new Property[CustomUserStoreConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.size()]));
        properties.setAdvancedProperties(CustomUserStoreConstants.CUSTOM_UM_ADVANCED_PROPERTIES.toArray
                (new Property[CustomUserStoreConstants.CUSTOM_UM_ADVANCED_PROPERTIES.size()]));
        return properties;
    }
}