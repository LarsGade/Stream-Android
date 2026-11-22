package dk.stream.android;

import android.app.Activity;
import android.graphics.Color;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

/** Music connection settings live in the app's Settings menu. */
final class MusicSourceSettings {
 interface Save {boolean save(String address,String proxy);}
 static void show(Activity activity,Library library,Save save){
  LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);int unit=Math.round(activity.getResources().getDisplayMetrics().density);content.setPadding(20*unit,8*unit,20*unit,0);
  EditText address=new EditText(activity),proxy=new EditText(activity);
  for(EditText input:new EditText[]{address,proxy}){input.setSingleLine();input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);}
  address.setText(library.openSpotApiUrl);proxy.setText(library.openSpotProxyUrl);
  TextView addressLabel=new TextView(activity);addressLabel.setText(UiText.text("API-adresse"));addressLabel.setTextColor(Color.WHITE);content.addView(addressLabel);content.addView(address);
  TextView proxyLabel=new TextView(activity);proxyLabel.setText(UiText.text("Søgeserver"));proxyLabel.setTextColor(Color.WHITE);content.addView(proxyLabel);content.addView(proxy);
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(UiText.text("Kilde til musik")).setView(content).setNegativeButton(UiText.text("Annuller"),null).setNeutralButton(UiText.text("Standard"),(picker,which)->save.save(OpenSpotSaavnApi.DEFAULT,OpenSpotSaavnApi.PROXY)).setPositiveButton(UiText.text("Gem"),null).create();
  dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{try{String base=OpenSpotSaavnApi.endpoint(address.getText().toString()).toString();String relay=OpenSpotSaavnApi.endpoint(proxy.getText().toString()).toString();if(save.save(base,relay))dialog.dismiss();}catch(IllegalArgumentException invalid){UiText.toast(activity,invalid.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
 }
 private MusicSourceSettings(){}
}
