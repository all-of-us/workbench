import UserAdminPage from 'app/page/admin-user-list-page';
import {signInWithAccessToken} from 'utils/test-utils';
import navigation, { NavLink } from 'app/component/navigation';
import AdminTable from 'app/component/admin-table';


let username = "admin_test@fake-research-aou.org";

describe('Admin', () => {

    beforeEach(async () => {
      await signInWithAccessToken(page);
      //await navigation.navMenu(page, NavLink.ADMIN);
      await navigation.navMenu(page, NavLink.USER_ADMIN);
      
    });

    test('admin able to update bypass access', async () => { 
      let userAdminPage = new UserAdminPage(page);
      await userAdminPage.waitForLoad();
      //look up for the user
      await userAdminPage.searchUser(username);
      await userAdminPage.waitForLoad();

      //Verify table column names match.
       const columns = [
          'Status',
          'Institution',
          'Registration date',
          'User name',
          'Contact Email',
          'User Lockout',
          'First Sign-in',
          '2FA',
          'Training',
          'eRA Commons',
          'DUCC',
          'Bypass',
          'Audit'
        ]
        let adminTable = new AdminTable(page);
        const columnNames = await adminTable.getColumnNames();
        console.log(columnNames);
        expect(columnNames).toHaveLength(columns.length);
        expect(columnNames.sort()).toEqual(columns.sort());
       
      // const usernameIndex = await adminTable.getUsernameIndex(username);
      //   const columnIndex = await adminTable.bypassLinkColindex()
      //   console.log(columnIndex);

      // //   //click on the bypass link
      //   let bypassLinkModal  = await userAdminPage.clickBypassLink(1, columnIndex);
      //  let toggleText = await bypassLinkModal.getAllToggleTexts();
      //  console.log(toggleText);
    

      // // change toggle to green for all the modules
      //  await bypassLinkModal.bypassAllModules();

      //get the index of the user lockout column
      const userLockoutColIndex = await adminTable.getColumnIndex("User Lockout");
      console.log(`userLockoutColIndex: ${userLockoutColIndex}`);
      //click to disable the user
       await userAdminPage.clickUserLockout(1, userLockoutColIndex);
       await userAdminPage.waitForLoad();

       const statusColIndex = await adminTable.getColumnIndex("Status");
       //verify that the status col has status as active and user lockout displays ENABLE
      const userStatus = await userAdminPage.getStatusText(1, statusColIndex);
      console.log(`userStatus: ${userStatus}`);

      const auditColIndex = await adminTable.getColumnIndex("Audit");
      console.log(`auditColIndex: ${auditColIndex}`);
       //verify that the status col has status as active and user lockout displays ENABLE
     await userAdminPage.clickAuditLink(1, auditColIndex);

      const nameColIndex = await adminTable.getNameColindex();

       //click on the name link to navigate to the user profile page
       console.log(nameColIndex);
       await userAdminPage.clickNameLink(1, nameColIndex); 
       
       // declare new tab, now you can work with it
      
        // verify that all the modules display a checkmark in the respective columns

        // click on the user name to navigate to the user profile info page

         // Verify "Save Profile" button is disabled first time page is opened.
        //await userProfileInfo.waitForSaveButton(false);

        // verify that all the modules are displaying the green toggle

    });

    test.skip('admin able to update the free credits', async () => { 

    //verify that the default free credit is displaying

    //verify that the admin is able to update the free credits
    });

    test.skip('admin able to disable and enable the user', async () => { 

        //verify that the User Lockout column is displaying DISABLE (default state)

        //verify that the status column is displaying Active (default status)
    
        //verify that the admin is able to update the free credits
        });


});