package com.interstellarstudios.note_ify.models;

public class Details {

    private String profilePic;

    public Details() {
        //empty constructor needed
    }

    public Details(String profilePic) {

        this.profilePic = profilePic;
    }

    public String getProfilePic() {
        return profilePic;
    }
}