package luolc.project.filemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class FileManager extends Activity {

    private static final int MAX_PATH_LENGTH = 35;

    private static final int ITEM_ID_DELETE_FILE = 10000;

    private static final String[][] MIME_MAP = {
            {".apk", "application/vnd.android.package-archive"},
            {".avi", "video/x-msvideo"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".flv", "video/mpeg"},
            {".gif", "image/gif"},
            {".html", "text/html"},
            {".jpg", "image/jpeg"},
            {".mov", "video/quicktime"},
            {".mp3", "audio/mpeg"},
            {".mpg", "video/mpeg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".rar", "application/octet-stream"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".zip", "application/zip"}
    };

    private Context mContext;

    private double exitTime;
    private String rootPath;
    private String currentPath;
    private File currentFile;
    private File selectedFile;
    private List<File> files;
    private List<Map<String, Object>> items;

    private TextView tvPath;
    private ListView lvFile;
    private ImageView imHome;
    private ImageButton btnActionBack;
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_file_manager);

        initConfig();
        initView();
        showDirectory();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, ITEM_ID_DELETE_FILE, 0, R.string.file_manager_menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ITEM_ID_DELETE_FILE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.file_manager_dialog_title_delete)
                        .setMessage(R.string.file_manager_dialog_delete_message)
                        .setPositiveButton(R.string.file_manager_dialog_button_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if(deleteAllFiles(selectedFile)) {
                                            Toast.makeText(mContext, R.string.file_manager_dialog_toast_delete_success,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(mContext, R.string.file_manager_dialog_toast_delete_fail,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        showDirectory();
                                    }
                                })
                        .setNegativeButton(R.string.file_manager_dialog_button_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                        .create().show();

        }
        return super.onContextItemSelected(item);
    }

    private boolean deleteAllFiles(File root) {
        try {
            File files[] = root.listFiles();
            if(files != null) {
                for(File file : files) {
                    deleteAllFiles(file);
                }
            }
            return root.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void initConfig() {
        exitTime = 0.0D;
        currentFile = Environment.getExternalStorageDirectory();
        rootPath = currentFile.getPath();
        currentPath = rootPath;
        items = new ArrayList<>();
    }

    private void initView() {
        tvPath = (TextView) findViewById(R.id.tv_path);
        lvFile = (ListView) findViewById(R.id.lv_file_list);
        imHome = (ImageView) findViewById(R.id.im_home);
        btnActionBack = (ImageButton) findViewById(R.id.btn_action_back);

        adapter = new SimpleAdapter(this, items, R.layout.item_file_list,
                new String[]{"category", "file_name"}, new int[]{R.id.im_category, R.id.tv_file_name});
        lvFile.setAdapter(adapter);
        lvFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (files.get(position).isDirectory()) {
                    if (files.get(position).listFiles() != null) {
                        currentPath = files.get(position).getPath();
                        currentFile = new File(currentPath);
                        showDirectory();
                    }
                    return;
                }
                try {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction("android.intent.action.VIEW");
                    String type = getMIMEType(files.get(position));
                    intent.setDataAndType(Uri.fromFile(files.get(position)), type);
//                    Toast.makeText(mContext, type, Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.file_manager_toast_unable_to_open,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        lvFile.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedFile = files.get(position);
                return false;
            }
        });
        registerForContextMenu(lvFile);
    }

    private void updateHeaderView() {
        if (currentPath.equals(rootPath)) {
            imHome.setVisibility(View.VISIBLE);
            btnActionBack.setVisibility(View.GONE);
        } else {
            imHome.setVisibility(View.GONE);
            btnActionBack.setVisibility(View.VISIBLE);
        }
    }

    private void showDirectory() {
        if (currentPath == null || "".equals(currentPath)) {
            currentPath = rootPath;
            currentFile = new File(rootPath);
        }
        updateHeaderView();
        String subPath = currentPath;
        if (subPath.length() > MAX_PATH_LENGTH) {
            subPath = subPath.substring(subPath.length() - MAX_PATH_LENGTH);
            subPath = "..." + subPath;
        }
        tvPath.setText(subPath);
        files = Arrays.asList(currentFile.listFiles());
        resetItemSource();
        adapter.notifyDataSetChanged();
    }

    private void resetItemSource() {
        if (files == null) {
            return;
        }

        items.clear();
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
        for (int i = 0; i < files.size(); ++i) {
            Map<String, Object> map = new HashMap<>();
            map.put("file_name", files.get(i).getName());
            if (files.get(i).isDirectory()) {
                map.put("category", R.drawable.files_folder_icon);
                items.add(map);
                continue;
            }

            String suffix = getSuffix(files.get(i));
            if (".apk".equals(suffix)) {
                map.put("category", R.drawable.files_apk_icon);
            } else if (".avi".equals(suffix)) {
                map.put("category", R.drawable.files_avi_icon);
            } else if (".doc".equals(suffix)) {
                map.put("category", R.drawable.files_doc_icon);
            } else if (".docx".equals(suffix)) {
                map.put("category", R.drawable.files_doc_icon);
            } else if (".flv".equals(suffix)) {
                map.put("category", R.drawable.files_flv_icon);
            } else if (".gif".equals(suffix)) {
                map.put("category", R.drawable.files_gif_icon);
            } else if (".html".equals(suffix)) {
                map.put("category", R.drawable.files_html_icon);
            } else if (".jpg".equals(suffix)) {
                map.put("category", R.drawable.files_jpg_icon);
            } else if (".mov".equals(suffix)) {
                map.put("category", R.drawable.files_mov_icon);
            } else if (".mp3".equals(suffix)) {
                map.put("category", R.drawable.files_mp3_icon);
            } else if (".mpg".equals(suffix)) {
                map.put("category", R.drawable.files_mpg_icon);
            } else if (".pdf".equals(suffix)) {
                map.put("category", R.drawable.files_pdf_icon);
            } else if (".png".equals(suffix)) {
                map.put("category", R.drawable.files_png_icon);
            } else if (".ppt".equals(suffix)) {
                map.put("category", R.drawable.files_ppt_icon);
            } else if (".pptx".equals(suffix)) {
                map.put("category", R.drawable.files_ppt_icon);
            } else if (".rar".equals(suffix)) {
                map.put("category", R.drawable.files_rar_icon);
            } else if (".txt".equals(suffix)) {
                map.put("category", R.drawable.files_txt_icon);
            } else if (".wav".equals(suffix)) {
                map.put("category", R.drawable.files_wav_icon);
            } else if (".wma".equals(suffix)) {
                map.put("category", R.drawable.files_wma_icon);
            } else if (".xls".equals(suffix)) {
                map.put("category", R.drawable.files_xls_icon);
            } else if (".xlsx".equals(suffix)) {
                map.put("category", R.drawable.files_xls_icon);
            } else if (".zip".equals(suffix)) {
                map.put("category", R.drawable.files_zip_icon);
            } else {
                map.put("category", R.drawable.files_unknown_icon);
            }
            items.add(map);
        }
    }

    String getSuffix(File file) {
        String fileName;
        int lastDot;
        String suffix = "";

        fileName = file.getName();
        lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            suffix = fileName.substring(lastDot).toLowerCase(Locale.getDefault());
        }
        if (".".equals(suffix)) {
            suffix = "";
        }
        return suffix;
    }

    private String getMIMEType(File file) {
        String type = "*/*";
        String suffix = getSuffix(file);

        if ("".equals(suffix)) {
            return type;
        }
        for (int i = 0; i < MIME_MAP.length; ++i) {
            if (suffix.equals(MIME_MAP[i][0])) {
                type = MIME_MAP[i][1];
                break;
            }
        }
        return type;
    }

    private void actionBack() {
        currentFile = currentFile.getParentFile();
        currentPath = currentFile.getPath();
        showDirectory();
    }

    public void onClickBackButton(View view) {
        actionBack();
    }

    public void onClickNewFolder(View view) {
        final RelativeLayout layout = (RelativeLayout) View.inflate(this, R.layout.dialog_new_folder, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout).setTitle(R.string.file_manager_dialog_title_new_folder)
                .setPositiveButton(R.string.file_manager_dialog_button_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Field field = dialog.getClass()
                                            .getSuperclass().getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(dialog, false);
                                } catch (Exception ignore) {}

                                EditText etNewName;
                                etNewName = (EditText) layout.findViewById(R.id.et_new_folder_name);
                                String fileName = etNewName.getText().toString();
                                if("".equals(fileName)) {
                                    Toast.makeText(mContext, R.string.file_manager_dialog_toast_empty,
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if(fileName.contains("/") || fileName.contains("\\")
                                        || fileName.contains(":") || fileName.contains("*")
                                        || fileName.contains("?") || fileName.contains("\"")
                                        || fileName.contains("<") || fileName.contains(">")
                                        || fileName.contains("|")) {
                                    Toast.makeText(mContext, R.string.file_manager_dialog_toast_invalid_character,
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                File newFile = new File(currentPath + "/" + fileName);
                                if(newFile.exists()) {
                                    Toast.makeText(mContext, R.string.file_manager_dialog_toast_file_exist,
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                try {
                                    Field field = dialog.getClass()
                                            .getSuperclass().getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(dialog, true);
                                } catch (Exception ignore) {}
                                dialog.dismiss();
                                newFile.mkdirs();
                                showDirectory();
                            }
                        })
                .setNegativeButton(R.string.file_manager_dialog_button_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Field field = dialog.getClass()
                                            .getSuperclass().getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(dialog, true);
                                } catch (Exception ignore) {
                                }
                            }
                        }).create().show();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!currentPath.equals(rootPath)) {
                actionBack();
                return true;
            }
            if (System.currentTimeMillis() - exitTime > 2000.0D) {
                Toast.makeText(this, R.string.file_manager_toast_exit, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
                return true;
            }
            exitTime = 0.0D;
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
