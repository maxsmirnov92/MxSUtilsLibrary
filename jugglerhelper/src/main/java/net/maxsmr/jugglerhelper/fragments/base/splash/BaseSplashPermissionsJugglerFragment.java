package net.maxsmr.jugglerhelper.fragments.base.splash;

public abstract class BaseSplashPermissionsJugglerFragment extends BaseSplashJugglerFragment {

    // TODO

//    private static final String ARG_IS_SETTINGS_SCREEN_SHOWED = BaseSplashPermissionsJugglerFragment.class.getName() + ".ARG_IS_SETTINGS_SCREEN_SHOWED";
//
//    private final Stack<Dialog> grantedDialogs = new Stack<>();
//    private final Stack<Dialog> deniedDialogs = new Stack<>();
//
//    private boolean isExitOnPositiveClickSet = false;
//    private boolean isSettingsScreenShowedOnce = false;
//
//        @NonNull
//    private Dialog createPermissionAlertDialog(String permission, final boolean granted, DialogInterface.OnClickListener positiveClickListener) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setMessage(
//                String.format(granted ? getString(R.string.dialog_message_permission_granted) :
//                                getString(R.string.dialog_message_permission_denied),
//                        permission))
//                .setCancelable(false)
//                .setPositiveButton(android.R.string.ok, positiveClickListener)
//                .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @SuppressWarnings("SuspiciousMethodCalls")
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        if (granted && grantedDialogs.contains(dialog)) {
//                            grantedDialogs.remove(dialog);
//                        } else {
//                            deniedDialogs.remove(dialog);
//                            if (deniedDialogs.isEmpty()) {
//                                isExitOnPositiveClickSet = false;
//                            }
//                        }
//                    }
//                });
//        return builder.create();
//    }
//
//    @NonNull
//    private Dialog createNoPermissionsAlertDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        builder.setMessage(R.string.dialog_message_permissions_empty)
//                .setCancelable(true)
//                .setPositiveButton(android.R.string.ok, null)
//                .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        dialog.dismiss();
//                        getActivity().finish();
//                        System.exit(0);
//                    }
//                });
//        return builder.create();
//    }

}
