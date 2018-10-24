package com.freeme.dialer.contacts.merge;

import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;

import java.util.Arrays;
import java.util.LinkedHashMap;

class FreemeContactData {

    private long mId;
    private String mDisplayName;
    private String mNickName;
    private byte[] mPhoto;
    private String mAccountType;
    private String mAccountName;

    private LinkedHashMap<Long, String> mDisplayNames;
    private LinkedHashMap<Long, String> mNickNames;
    private LinkedHashMap<Long, Photo> mPhotos;
    private LinkedHashMap<String, Phone> mPhones;
    private LinkedHashMap<String, Email> mEmails;
    private LinkedHashMap<String, IM> mIms;
    private LinkedHashMap<String, Address> mAddresses;
    private LinkedHashMap<String, Company> mCompanies;
    private LinkedHashMap<String, Website> mWebsites;
    private LinkedHashMap<String, Note> mNotes;
    private LinkedHashMap<String, Birthday> mBirthdaies;
    private LinkedHashMap<String, GroupMembership> mGroups;

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public String getNickName() {
        return mNickName;
    }

    public void setNickName(String nickName) {
        this.mNickName = nickName;
    }

    public byte[] getPhoto() {
        return mPhoto;
    }

    public void setPhoto(byte[] photo) {
        this.mPhoto = photo;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public void setAccountType(String accountType) {
        this.mAccountType = accountType;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public void setAccountName(String accountName) {
        this.mAccountName = accountName;
    }

    public LinkedHashMap<Long, String> getDisplayNames() {
        if (mDisplayNames == null) {
            mDisplayNames = new LinkedHashMap<>();
        }
        return mDisplayNames;
    }

    public void setDisplayNames(LinkedHashMap<Long, String> displayNames) {
        this.mDisplayNames = displayNames;
    }

    public LinkedHashMap<Long, String> getNickNames() {
        if (mNickNames == null) {
            mNickNames = new LinkedHashMap<>();
        }
        return mNickNames;
    }

    public void setNickNames(LinkedHashMap<Long, String> nickNames) {
        this.mNickNames = nickNames;
    }

    public LinkedHashMap<Long, Photo> getPhotos() {
        if (mPhotos == null) {
            mPhotos = new LinkedHashMap<>();
        }
        return mPhotos;
    }

    public void setPhotos(LinkedHashMap<Long, Photo> mPhotos) {
        this.mPhotos = mPhotos;
    }

    public LinkedHashMap<String, Phone> getPhones() {
        if (mPhones == null) {
            mPhones = new LinkedHashMap<>();
        }
        return mPhones;
    }

    public void setPhones(LinkedHashMap<String, Phone> phones) {
        this.mPhones = phones;
    }

    public LinkedHashMap<String, Email> getEmails() {
        if (mEmails == null) {
            mEmails = new LinkedHashMap<>();
        }
        return mEmails;
    }

    public void setEmails(LinkedHashMap<String, Email> emails) {
        this.mEmails = emails;
    }

    public LinkedHashMap<String, IM> getIms() {
        if (mIms == null) {
            mIms = new LinkedHashMap<>();
        }
        return mIms;
    }

    public void setIms(LinkedHashMap<String, IM> ims) {
        this.mIms = ims;
    }

    public LinkedHashMap<String, Address> getAddresses() {
        if (mAddresses == null) {
            mAddresses = new LinkedHashMap<>();
        }
        return mAddresses;
    }

    public void setAddresses(LinkedHashMap<String, Address> addresses) {
        this.mAddresses = addresses;
    }

    public LinkedHashMap<String, Company> getCompanies() {
        if (mCompanies == null) {
            mCompanies = new LinkedHashMap<>();
        }
        return mCompanies;
    }

    public void setCompanies(LinkedHashMap<String, Company> companies) {
        this.mCompanies = companies;
    }

    public LinkedHashMap<String, Website> getWebsites() {
        if (mWebsites == null) {
            mWebsites = new LinkedHashMap<>();
        }
        return mWebsites;
    }

    public void setWebsites(LinkedHashMap<String, Website> websites) {
        this.mWebsites = websites;
    }

    public LinkedHashMap<String, Note> getNotes() {
        if (mNotes == null) {
            mNotes = new LinkedHashMap<>();
        }
        return mNotes;
    }

    public void setNotes(LinkedHashMap<String, Note> notes) {
        this.mNotes = notes;
    }

    public LinkedHashMap<String, Birthday> getBirthdaies() {
        if (mBirthdaies == null) {
            mBirthdaies = new LinkedHashMap<>();
        }
        return mBirthdaies;
    }

    public void setBirthdaies(LinkedHashMap<String, Birthday> birthdaies) {
        this.mBirthdaies = birthdaies;
    }

    public LinkedHashMap<String, GroupMembership> getGroups() {
        if (mGroups == null) {
            mGroups = new LinkedHashMap<>();
        }
        return mGroups;
    }

    public void setGroups(LinkedHashMap<String, GroupMembership> groups) {
        this.mGroups = groups;
    }

    public static class Base {

        static final String TYPE_UNKNOWN = "-1";

        String mContent;
        String mType = TYPE_UNKNOWN;

        public Base(String content) {
            this.mContent = content;
        }

        public Base(String content, String type) {
            this.mContent = content;
            this.mType = type;
        }

        public String getContent() {
            return mContent;
        }

        public void setContent(String mContent) {
            this.mContent = mContent;
        }

        public String getType() {
            return mType;
        }

        public void setType(String type) {
            this.mType = type;
        }

        public String toString(Base base, String appendContent) {
            return base.getClass().getName()
                    + " [ Content:" + mContent + ", "
                    + " Type: " + mType + ", "
                    + appendContent + " ]";
        }
    }

    public static class Phone extends Base {

        String mLabel;

        public Phone() {
            this(null, TYPE_UNKNOWN, null);
        }

        public Phone(String number, String type, String label) {
            super(number, type);
            this.mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        public void setLabel(String label) {
            this.mLabel = label;
        }

        @Override
        public String toString() {
            return super.toString(this, "Label: " + mLabel);
        }
    }

    public static class Email extends Base {

        public Email() {
            this(null, TYPE_UNKNOWN);
        }

        public Email(String addres, String type) {
            super(addres, type);
        }

        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class IM extends Base {

        public IM() {
            this(null, String.valueOf(Im.PROTOCOL_CUSTOM));
        }

        public IM(String name, String type) {
            super(name, type);
        }

        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class Address extends Base {

        public Address() {
            this(null, TYPE_UNKNOWN);
        }

        public Address(String address, String type) {
            super(address, type);
        }

        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class Company extends Base {

        String mPosition;

        public Company() {
            this(null, TYPE_UNKNOWN, null);
        }

        public Company(String company, String type, String position) {
            super(company, type);
            this.mPosition = position;
        }


        public String getPosition() {
            return mPosition;
        }

        public void setPosition(String position) {
            this.mPosition = position;
        }

        @Override
        public String toString() {
            return super.toString(this, "Position: " + mPosition);
        }
    }

    public static class Website extends Base {

        public Website() {
            this(null);
        }

        public Website(String web) {
            super(web);
        }


        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class Note extends Base {

        public Note() {
            this(null);
        }

        public Note(String web) {
            super(web);
        }


        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class GroupMembership extends Base {

        public GroupMembership() {
            this(null, TYPE_UNKNOWN);
        }

        public GroupMembership(String groupName, String groupId) {
            super(groupName, groupId);
        }


        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class Photo extends Base {

        private byte[] mPhoto;
        private long mContactId;

        public Photo() {
            this(null, 0);
        }

        public Photo(byte[] photo, long contactId) {
            super(null, TYPE_UNKNOWN);
            mPhoto = photo;
            mContactId = contactId;
        }

        public long getContactId() {
            return mContactId;
        }

        public byte[] getPhoto() {
            return mPhoto;
        }

        @Override
        public String toString() {
            return super.toString(this, "ContactId: " + mContactId + ", mPhoto: " + mPhoto);
        }
    }

    public static class Events extends Base {

        public Events() {
            this(null, TYPE_UNKNOWN);
        }

        public Events(String event, String type) {
            super(event, type);
        }

        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    public static class Birthday extends Events {

        public Birthday() {
            this(null);
        }

        public Birthday(String birthday) {
            super(birthday, String.valueOf(Event.TYPE_BIRTHDAY));
        }

        @Override
        public String toString() {
            return super.toString(this, null);
        }
    }

    @Override
    public String toString() {
        return "FreemeContactData {"
                + " AccountType=" + mAccountType
                + ", AccountName=" + mAccountName
                + ", Id: " + mId
                + ", DisplayName=" + mDisplayName
                + ", NickName=" + mNickName
                + ", Photo=" + Arrays.toString(mPhoto)
                + ", Phones=" + mPhones
                + ", Emails=" + mEmails
                + ", Ims=" + mIms
                + ", Addresses=" + mAddresses
                + ", Companies=" + mCompanies
                + ", Webs=" + mWebsites
                + "}";
    }
}