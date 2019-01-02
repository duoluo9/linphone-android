package org.linphone.contacts;

/*
SearchContactsAdapter.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.core.Address;
import org.linphone.core.FriendCapability;
import org.linphone.core.PresenceBasicStatus;
import org.linphone.core.PresenceModel;
import org.linphone.core.ProxyConfig;
import org.linphone.core.SearchResult;
import org.linphone.views.ContactAvatar;

public class SearchContactsAdapter extends RecyclerView.Adapter<SearchContactViewHolder> {
    private List<SearchResult> mContacts;
    private ArrayList<ContactAddress> mContactsSelected;
    private boolean mOnlySipContact = false;
    private SearchContactViewHolder.ClickListener mListener;
    private final boolean mIsOnlyOnePersonSelection;
    private String mPreviousSearch;
    private boolean mSecurityEnabled;

    public SearchContactsAdapter(
            SearchContactViewHolder.ClickListener clickListener,
            boolean hideSelectionMark,
            boolean isSecurityEnabled) {
        mIsOnlyOnePersonSelection = hideSelectionMark;
        mListener = clickListener;
        setContactsSelectedList(null);
        mPreviousSearch = null;
        mSecurityEnabled = isSecurityEnabled;
        mContacts = new ArrayList<>();
    }

    public List<SearchResult> getContacts() {
        return mContacts;
    }

    public void setOnlySipContact(boolean enable) {
        mOnlySipContact = enable;
    }

    public void setSecurityEnabled(boolean enable) {
        mSecurityEnabled = enable;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.search_contact_cell, parent, false);
        return new SearchContactViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchContactViewHolder holder, int position) {
        SearchResult searchResult = getItem(position);

        LinphoneContact contact;
        if (searchResult.getAddress() == null) {
            contact =
                    ContactsManager.getInstance()
                            .findContactFromPhoneNumber(searchResult.getPhoneNumber());
        } else {
            contact =
                    ContactsManager.getInstance().findContactFromAddress(searchResult.getAddress());
        }

        final String numberOrAddress =
                (searchResult.getPhoneNumber() != null)
                        ? searchResult.getPhoneNumber()
                        : searchResult.getAddress().asStringUriOnly();

        holder.name.setVisibility(View.GONE);
        if (contact != null && contact.getFullName() != null) {
            holder.name.setVisibility(View.VISIBLE);
            holder.name.setText(contact.getFullName());
        } else if (searchResult.getAddress() != null) {
            if (searchResult.getAddress().getUsername() != null) {
                holder.name.setVisibility(View.VISIBLE);
                holder.name.setText(searchResult.getAddress().getUsername());
            } else if (searchResult.getAddress().getDisplayName() != null) {
                holder.name.setVisibility(View.VISIBLE);
                holder.name.setText(searchResult.getAddress().getDisplayName());
            }
        } else if (searchResult.getAddress() != null) {
            holder.name.setVisibility(View.VISIBLE);
            holder.name.setText(
                    (searchResult.getAddress().getDisplayName() != null)
                            ? searchResult.getAddress().getDisplayName()
                            : searchResult.getAddress().getUsername());
        }

        holder.disabled.setVisibility(View.GONE);
        if (contact != null) {
            if (contact.getFullName() == null
                    && contact.getFirstName() == null
                    && contact.getLastName() == null) {
                contact.setFullName(holder.name.getText().toString());
            }
            ContactAvatar.displayAvatar(
                    contact,
                    contact.hasFriendCapability(FriendCapability.LimeX3Dh),
                    holder.avatarLayout);

            if ((!mIsOnlyOnePersonSelection
                            && !searchResult.hasCapability(FriendCapability.GroupChat))
                    || (mSecurityEnabled
                            && !searchResult.hasCapability(FriendCapability.LimeX3Dh))) {
                // Disable row, contact doesn't have the required capabilities
                holder.disabled.setVisibility(View.VISIBLE);
            }
        } else {
            ContactAvatar.displayAvatar(holder.name.getText().toString(), holder.avatarLayout);
        }

        holder.address.setText(numberOrAddress);
        if (holder.linphoneContact != null) {
            holder.linphoneContact.setVisibility(View.GONE);
            if (searchResult.getFriend() != null
                    && contact != null
                    && contact.getBasicStatusFromPresenceModelForUriOrTel(numberOrAddress)
                            == PresenceBasicStatus.Open) {
                holder.linphoneContact.setVisibility(View.VISIBLE);
            }
        }
        if (holder.isSelect != null) {
            if (isContactSelected(searchResult)) {
                holder.isSelect.setVisibility(View.VISIBLE);
            } else {
                holder.isSelect.setVisibility(View.INVISIBLE);
            }
            if (mIsOnlyOnePersonSelection) {
                holder.isSelect.setVisibility(View.GONE);
            }
        }
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean isContactSelected(SearchResult sr) {
        for (ContactAddress c : mContactsSelected) {
            Address addr = c.getAddress();
            if (addr != null && sr.getAddress() != null) {
                if (addr.weakEqual(sr.getAddress())) {
                    return true;
                }
            } else {
                if (c.getPhoneNumber() != null && sr.getPhoneNumber() != null) {
                    if (c.getPhoneNumber().compareTo(sr.getPhoneNumber()) == 0) return true;
                }
            }
        }
        return false;
    }

    public ArrayList<ContactAddress> getContactsSelectedList() {
        return mContactsSelected;
    }

    public void setContactsSelectedList(ArrayList<ContactAddress> contactsList) {
        if (contactsList == null) {
            mContactsSelected = new ArrayList<>();
        } else {
            mContactsSelected = contactsList;
        }
    }

    public boolean toggleContactSelection(ContactAddress ca) {
        if (mContactsSelected.contains(ca)) {
            mContactsSelected.remove(ca);
            return false;
        } else {
            mContactsSelected.add(ca);
            return true;
        }
    }

    private SearchResult getItem(int position) {
        return mContacts.get(position);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public void searchContacts(String search) {
        List<SearchResult> result = new ArrayList<>();

        if (mPreviousSearch != null) {
            if (mPreviousSearch.length() > search.length()) {
                ContactsManager.getInstance().getMagicSearch().resetSearchCache();
            }
        }
        mPreviousSearch = search;

        String domain = "";
        ProxyConfig prx = LinphoneManager.getLc().getDefaultProxyConfig();
        if (prx != null) domain = prx.getDomain();
        SearchResult[] searchResults =
                ContactsManager.getInstance()
                        .getMagicSearch()
                        .getContactListFromFilter(search, mOnlySipContact ? domain : "");

        for (SearchResult sr : searchResults) {
            if (LinphoneActivity.instance()
                    .getResources()
                    .getBoolean(R.bool.hide_sip_contacts_without_presence)) {
                if (sr.getFriend() != null) {
                    PresenceModel pm =
                            sr.getFriend()
                                    .getPresenceModelForUriOrTel(sr.getAddress().asStringUriOnly());
                    if (pm != null && pm.getBasicStatus().equals(PresenceBasicStatus.Open)) {
                        result.add(sr);
                    } else {
                        pm = sr.getFriend().getPresenceModelForUriOrTel(sr.getPhoneNumber());
                        if (pm != null && pm.getBasicStatus().equals(PresenceBasicStatus.Open)) {
                            result.add(sr);
                        }
                    }
                }
            } else {
                result.add(sr);
            }
        }

        mContacts = result;
        notifyDataSetChanged();
    }
}
