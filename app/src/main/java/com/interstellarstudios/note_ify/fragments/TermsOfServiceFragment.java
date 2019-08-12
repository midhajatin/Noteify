package com.interstellarstudios.note_ify.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.interstellarstudios.note_ify.R;
import static android.content.Context.MODE_PRIVATE;

public class TermsOfServiceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terms_of_service, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextView termsOfService = getView().findViewById(R.id.termsOfService);

        String termsText = ("Please read these terms of service (\"terms\", \"terms of service\") carefully before using any Application or website (the \"service\") operated by Interstellar Studios (\"us\", 'we\", \"our\").\n" +
                "\n" +
                "Conditions of Use\n" +
                "\n" +
                "We will provide services to you, which are subject to the conditions stated below in this document. Every time you use the service, you accept the following conditions. This is why we urge you to read them carefully.\n" +
                "\n" +
                "Privacy Policy\n" +
                "\n" +
                "Before you continue using the service we advise you to read our privacy policy, found on our Apps and websites, regarding our user data collection. It will help you better understand our practices.\n" +
                "\n" +
                "Copyright\n" +
                "\n" +
                "Content published on our Apps or websites (digital downloads, images, texts, graphics, logos) is the property of Interstellar Studios and/or its content creators and protected by international copyright laws. The entire compilation of the content found on the app is the exclusive property of Interstellar Studios with copyright authorship for this compilation by Interstellar Studios.\n" +
                "\n" +
                "Communications\n" +
                "\n" +
                "Communication with us is electronic in its entirety. Every time you send us an email or use our the service, you are going to be communicating with us. You hereby consent to receive communications from us. We will continue to communicate with you by posting news and notices on our apps or websites. You also agree that all notices, disclosures, agreements and other communications we provide to you electronically meet the legal requirements that such communications be in writing.\n" +
                "\n" +
                "Applicable Law\n" +
                "\n" +
                "By using the services, you agree that the laws of the United Kingdom, without regard to principles of conflict laws, will govern these terms of service, or any dispute of any sort that might come between us and you, or its business partners and associates.\n" +
                "\n" +
                "Disputes\n" +
                "\n" +
                "Any dispute related in any way to your use of the service shall be arbitrated by the United Kingdom County Court and you consent to exclusive jurisdiction and venue of such courts.\n" +
                "\n" +
                "Comments, Reviews, and Emails\n" +
                "\n" +
                "Visitors may post content as long as it is not obscene, illegal, defamatory, threatening, infringing of intellectual property rights, invasive of privacy or injurious in any other way to third parties. Content has to be free of software viruses, political campaign, and commercial solicitation.\n" +
                "We reserve all rights (but not the obligation) to remove and/or edit such content. When you post your content, you grant us non-exclusive, royalty-free and irrevocable right to use, reproduce, publish or modify such content throughout the world in any media.\n" +
                "\n" +
                "License and Site Access\n" +
                "\n" +
                "We grant you a limited license to access and make personal use of the service. You are not allowed to download or modify it. This may be done only with written consent from us.\n" +
                "\n" +
                "User Account\n" +
                "\n" +
                "If you are an owner of an account on the service, you are solely responsible for maintaining the confidentiality of your private user details (username and password). You are responsible for all activities that occur under your account or password.\n" +
                "\n" +
                "We reserve all rights and have sole discretion to terminate accounts, edit or remove content.");

        termsOfService.setMovementMethod(new ScrollingMovementMethod());
        termsOfService.setText(termsText);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean switchThemesOnOff = sharedPreferences.getBoolean("switchThemes", true);

        if(switchThemesOnOff) {
            ConstraintLayout layout = getView().findViewById(R.id.container);
            layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDarkTheme));
            termsOfService.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDarkThemeText));
        }
    }
}
