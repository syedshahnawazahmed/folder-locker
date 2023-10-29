import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.Key;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import com.example.KeyGenerator;
public class SecureFolderLockerWithSwing {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new MainFrame("Secure Folder Locker");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 200);
            frame.setVisible(true);
        });
    }

    public static void lockFolder(String folderPath, char[] password) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            try {
                File encryptedFolder = new File(folderPath + ".locked");
                Key secretKey = KeyGenerator.generateKey(password); // Generate the key using the password
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                try (FileOutputStream fos = new FileOutputStream(encryptedFolder);
                     CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                    zipAndEncryptFolder(folder, cos);
                }

                if (deleteDirectory(folder)) {
                    JOptionPane.showMessageDialog(null, "Folder locked successfully.");
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to lock the folder.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error while locking the folder: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Folder not found or is not a directory.");
        }
    }

    public static void unlockFolder(String unlockPath, char[] unlockPassword) {
        File encryptedFolder = new File(unlockPath);
        String originalFolderPath = unlockPath.substring(0, unlockPath.length() - 7); // Remove ".locked"
        File originalFolder = new File(originalFolderPath);

        if (encryptedFolder.exists() && encryptedFolder.isFile()) {
            try {
                Key secretKey = KeyGenerator.generateKey(unlockPassword); // Generate the key using the password
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

                try (FileInputStream fis = new FileInputStream(encryptedFolder);
                     CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                    unzipAndDecryptFolder(cis, originalFolder);
                }

                if (encryptedFolder.delete()) {
                    JOptionPane.showMessageDialog(null, "Folder unlocked successfully.");
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to unlock the folder.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error while unlocking the folder: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Invalid password or folder not found.");
        }
    }

    private static void zipAndEncryptFolder(File source, OutputStream os) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            zipFiles(source, source, zos);
        }
    }

    private static void zipFiles(File root, File source, ZipOutputStream zos) throws IOException {
        for (File file : source.listFiles()) {
            if (file.isDirectory()) {
                zipFiles(root, file, zos);
            } else {
                String entryName = root.toURI().relativize(file.toURI()).getPath();
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private static void unzipAndDecryptFolder(InputStream is, File destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(is)) {
            unzipFiles(destination, zis);
        }
    }

    private static void unzipFiles(File destination, ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(destination, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
}

class MainFrame extends JFrame {
    private JLabel pathLabel;
    private JLabel passwordLabel;
    private JTextField folderPathField;
    private JPasswordField passwordField;
    private JButton lockButton;
    private JButton unlockButton;

    public MainFrame(String title) {
        super(title);
        setLayout(new GridBagLayout());

        pathLabel = new JLabel("Path:");
        passwordLabel = new JLabel("Password:");

        folderPathField = new JTextField(20);
        passwordField = new JPasswordField(20);
        lockButton = new JButton("Lock Folder");
        unlockButton = new JButton("Unlock Folder");

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.EAST;
        add(pathLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.WEST;
        add(folderPathField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.anchor = GridBagConstraints.EAST;
        add(passwordLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.WEST;
        add(passwordField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(lockButton, gc);

        gc.gridy = 3;
        add(unlockButton, gc);

        lockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String folderPath = folderPathField.getText();
                char[] password = passwordField.getPassword();
                SecureFolderLockerWithSwing.lockFolder(folderPath, password);
            }
        });

        unlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String folderPath = folderPathField.getText();
                char[] password = passwordField.getPassword();
                SecureFolderLockerWithSwing.unlockFolder(folderPath, password);
            }
        });
    }
}
