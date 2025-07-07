package services;

import com.google.firebase.database.*;
import models.Chat;
import models.Group;
import models.GroupSettings;
import models.GroupRole;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * GroupService - Service consolidé pour la gestion des groupes
 * Gère toutes les opérations liées aux groupes : création, membres, paramètres, etc.
 */
public class GroupService {
    private FirebaseService firebaseService;
    private ChatService chatService;
    private static GroupService instance;

    // Constructeur privé pour le pattern singleton
    private GroupService() {
        try {
            this.firebaseService = FirebaseService.getInstance();
            this.chatService = ChatService.getInstance();
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation de GroupService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Pattern singleton pour obtenir l'instance
    public static synchronized GroupService getInstance() {
        if (instance == null) {
            instance = new GroupService();
        }
        return instance;
    }

    /**
     * Créer un nouveau groupe
     * @param groupName Nom du groupe
     * @param description Description du groupe (optionnel)
     * @param creatorId ID de l'utilisateur créateur
     * @param initialMembers Liste des IDs des membres initiaux
     * @param isPublic Si le groupe est public
     * @return ID du groupe si créé avec succès, null sinon
     */
    public String createGroup(String groupName, String description, String creatorId,
                              List<String> initialMembers, boolean isPublic) {
        try {
            // Valider les entrées
            if (groupName == null || groupName.trim().isEmpty()) {
                System.err.println("Le nom du groupe ne peut pas être vide");
                return null;
            }

            if (creatorId == null || creatorId.trim().isEmpty()) {
                System.err.println("L'ID du créateur ne peut pas être vide");
                return null;
            }

            // Préparer la liste des membres
            List<String> members = new ArrayList<>();
            if (initialMembers != null) {
                members.addAll(initialMembers);
            }

            // S'assurer que le créateur est dans la liste
            if (!members.contains(creatorId)) {
                members.add(0, creatorId);
            }

            // Doit avoir au moins 2 membres pour un groupe
            if (members.size() < 2) {
                System.err.println("Le groupe doit avoir au moins 2 membres");
                return null;
            }

            // Utiliser le nom du groupe comme ID (nettoyer pour Firebase)
            String groupId = sanitizeGroupName(groupName.trim());
            
            // Vérifier l'unicité du nom
            if (groupExists(groupId)) {
                System.err.println("Un groupe avec ce nom existe déjà");
                return null;
            }

            // Créer l'objet Group
            Group group = new Group(groupId, groupName.trim(), creatorId);
            if (description != null && !description.trim().isEmpty()) {
                group.setDescription(description.trim());
            }
            group.setPublic(isPublic);

            // Ajouter tous les membres initiaux
            for (String memberId : members) {
                if (!memberId.equals(creatorId)) { // Le créateur est déjà ajouté dans le constructeur
                    group.addMember(memberId, creatorId);
                }
            }

            // Créer l'objet Chat correspondant
            Chat groupChat = new Chat(groupId, members, creatorId);
            groupChat.setChatName(groupName.trim());
            groupChat.setActive(true);

            // Sauvegarder dans Firebase
            CountDownLatch latch = new CountDownLatch(2);
            final boolean[] groupSuccess = {false};
            final boolean[] chatSuccess = {false};

            // Sauvegarder les données du groupe
            Map<String, Object> groupData = convertGroupToMap(group);
            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.setValue(groupData, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    groupSuccess[0] = true;
                } else {
                    System.err.println("Erreur lors de la création du groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            // Sauvegarder les données du chat
            Map<String, Object> chatData = convertChatToMap(groupChat);
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + groupId);
            chatRef.setValue(chatData, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    chatSuccess[0] = true;
                    // Ajouter le chat à la liste de chats de chaque membre
                    addChatToUsers(groupId, members);
                } else {
                    System.err.println("Erreur lors de la création du chat de groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(15, TimeUnit.SECONDS);

            if (groupSuccess[0] && chatSuccess[0]) {
                // Envoyer un message système de bienvenue
                String creatorName = getUserDisplayName(creatorId);
                sendSystemMessage(groupId, creatorName + " a créé le groupe \"" + groupName + "\"");
                return groupId;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la création du groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Charger les informations d'un groupe
     * @param groupId ID du groupe
     * @return Objet Group ou null si non trouvé
     */
    public Group loadGroup(String groupId) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final Group[] result = {null};

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        result[0] = convertMapToGroup(dataSnapshot);
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Erreur lors du chargement du groupe: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            return result[0];

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du groupe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtenir les groupes dont un utilisateur est membre
     * @param userId ID de l'utilisateur
     * @return Liste des groupes dont l'utilisateur est membre
     */
    public List<Group> getUserGroups(String userId) {
        try {
            List<Group> userGroups = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            DatabaseReference groupsRef = firebaseService.getDatabase().getReference("groups");
            groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                        try {
                            Group group = convertMapToGroup(groupSnapshot);

                            if (group != null && group.isActive() && group.isMember(userId)) {
                                userGroups.add(group);
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur lors de l'analyse du groupe: " + e.getMessage());
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Erreur lors du chargement des groupes: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(15, TimeUnit.SECONDS);

            // Trier par dernière activité (plus récente d'abord)
            userGroups.sort((g1, g2) -> Long.compare(g2.getLastActivity(), g1.getLastActivity()));

            return userGroups;

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des groupes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Ajouter un membre à un groupe existant
     * @param groupId ID du groupe
     * @param newMemberId ID de l'utilisateur à ajouter
     * @param addedBy ID de l'utilisateur effectuant l'action
     * @return true si le membre a été ajouté avec succès
     */
    public boolean addMemberToGroup(String groupId, String newMemberId, String addedBy) {
        try {
            // Charger les données du groupe
            Group group = loadGroup(groupId);
            if (group == null) {
                System.err.println("Groupe non trouvé: " + groupId);
                return false;
            }

            // Vérifier les permissions et ajouter le membre
            if (!group.addMember(newMemberId, addedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(2);
            final boolean[] success = {false};

            // Mettre à jour les données du groupe
            Map<String, Object> groupUpdates = new HashMap<>();
            groupUpdates.put("members", group.getMembers());
            groupUpdates.put("memberRoles/" + newMemberId, "MEMBER");
            groupUpdates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(groupUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    success[0] = true;
                } else {
                    System.err.println("Erreur lors de la mise à jour du groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            // Mettre à jour les participants du chat
            Map<String, Object> chatUpdates = new HashMap<>();
            chatUpdates.put("users", group.getMembers());

            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + groupId);
            chatRef.updateChildren(chatUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    // Ajouter le chat à la liste de chats du nouveau membre
                    DatabaseReference userChatsRef = firebaseService.getDatabase()
                            .getReference("users/" + newMemberId + "/chats");
                    userChatsRef.child(groupId).setValueAsync(true);
                } else {
                    System.err.println("Erreur lors de la mise à jour du chat: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                // Envoyer un message système
                String addedByName = getUserDisplayName(addedBy);
                String newMemberName = getUserDisplayName(newMemberId);
                sendSystemMessage(groupId, addedByName + " a ajouté " + newMemberName + " au groupe");
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout d'un membre au groupe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ajouter plusieurs membres à un groupe
     * @param groupId ID du groupe
     * @param userIds Liste des IDs à ajouter
     * @param adminId ID de l'admin effectuant l'action
     * @return true si au moins un membre a été ajouté
     */
    public boolean addMembersToGroup(String groupId, List<String> userIds, String adminId) {
        if (groupId == null || userIds == null || userIds.isEmpty() || adminId == null) {
            return false;
        }

        boolean anySuccess = false;
        for (String userId : userIds) {
            boolean success = addMemberToGroup(groupId, userId, adminId);
            if (success) {
                anySuccess = true;
            }
        }
        return anySuccess;
    }

    /**
     * Supprimer un membre d'un groupe
     * @param groupId ID du groupe
     * @param memberId ID du membre à supprimer
     * @param removedBy ID de l'utilisateur effectuant l'action
     * @return true si le membre a été supprimé avec succès
     */
    public boolean removeMemberFromGroup(String groupId, String memberId, String removedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null) {
                return false;
            }

            if (!group.removeMember(memberId, removedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(2);
            final boolean[] success = {false};

            // Mettre à jour les données du groupe
            Map<String, Object> groupUpdates = new HashMap<>();
            groupUpdates.put("members", group.getMembers());
            groupUpdates.put("admins", group.getAdmins());
            groupUpdates.put("memberRoles/" + memberId, null); // Supprimer le rôle
            groupUpdates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(groupUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    success[0] = true;
                } else {
                    System.err.println("Erreur lors de la mise à jour du groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            // Mettre à jour les participants du chat et supprimer de la liste de chats de l'utilisateur
            Map<String, Object> chatUpdates = new HashMap<>();
            chatUpdates.put("users", group.getMembers());

            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + groupId);
            chatRef.updateChildren(chatUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    // Supprimer le chat de la liste de chats du membre
                    DatabaseReference userChatsRef = firebaseService.getDatabase()
                            .getReference("users/" + memberId + "/chats/" + groupId);
                    userChatsRef.removeValueAsync();
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                String removedByName = getUserDisplayName(removedBy);
                String memberName = getUserDisplayName(memberId);

                if (removedBy.equals(memberId)) {
                    sendSystemMessage(groupId, memberName + " a quitté le groupe");
                } else {
                    sendSystemMessage(groupId, removedByName + " a supprimé " + memberName + " du groupe");
                }
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression d'un membre du groupe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Quitter un groupe (alias pour removeMemberFromGroup où le membre se supprime lui-même)
     * @param groupId ID du groupe
     * @param userId ID de l'utilisateur qui quitte
     * @return true si quitté avec succès
     */
    public boolean leaveGroup(String groupId, String userId) {
        return removeMemberFromGroup(groupId, userId, userId);
    }

    /**
     * Promouvoir un membre au rang d'administrateur
     * @param groupId ID du groupe
     * @param memberId Membre à promouvoir
     * @param promotedBy Utilisateur effectuant l'action
     * @return true si promu avec succès
     */
    public boolean promoteToAdmin(String groupId, String memberId, String promotedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null) {
                return false;
            }

            if (!group.promoteToAdmin(memberId, promotedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            Map<String, Object> updates = new HashMap<>();
            updates.put("admins", group.getAdmins());
            updates.put("memberRoles/" + memberId, "ADMIN");
            updates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(updates, (databaseError, databaseReference) -> {
                success[0] = (databaseError == null);
                if (databaseError != null) {
                    System.err.println("Erreur lors de la promotion du membre: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                String promotedByName = getUserDisplayName(promotedBy);
                String memberName = getUserDisplayName(memberId);
                sendSystemMessage(groupId, promotedByName + " a promu " + memberName + " au rang d'administrateur");
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la promotion du membre: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rétrograder un administrateur au rang de membre simple
     * @param groupId ID du groupe
     * @param adminId Admin à rétrograder
     * @param demotedBy Utilisateur effectuant l'action
     * @return true si rétrogradé avec succès
     */
    public boolean demoteFromAdmin(String groupId, String adminId, String demotedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null) {
                return false;
            }

            if (!group.demoteFromAdmin(adminId, demotedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            Map<String, Object> updates = new HashMap<>();
            updates.put("admins", group.getAdmins());
            updates.put("memberRoles/" + adminId, "MEMBER");
            updates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(updates, (databaseError, databaseReference) -> {
                success[0] = (databaseError == null);
                if (databaseError != null) {
                    System.err.println("Erreur lors de la rétrogradation de l'admin: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                String demotedByName = getUserDisplayName(demotedBy);
                String adminName = getUserDisplayName(adminId);
                sendSystemMessage(groupId, demotedByName + " a retiré " + adminName + " du rôle d'administrateur");
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la rétrogradation de l'admin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Générer un nouveau code d'invitation pour le groupe
     * @param groupId ID du groupe
     * @param userId Utilisateur effectuant la demande (doit être admin)
     * @return Le nouveau code d'invitation si réussi, null sinon
     */
    public String generateNewInviteCode(String groupId, String userId) {
        try {
            Group group = loadGroup(groupId);
            if (group == null || !group.isAdmin(userId)) {
                return null;
            }

            String newInviteCode = group.generateInviteCode();

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.child("inviteCode").setValue(newInviteCode, (databaseError, databaseReference) -> {
                success[0] = (databaseError == null);
                if (databaseError != null) {
                    System.err.println("Erreur lors de la mise à jour du code d'invitation: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            return success[0] ? newInviteCode : null;

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du code d'invitation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Rejoindre un groupe avec un code d'invitation
     * @param inviteCode Code d'invitation du groupe
     * @param userId ID de l'utilisateur souhaitant rejoindre
     * @return true si rejoint avec succès
     */
    public boolean joinGroupByInviteCode(String inviteCode, String userId) {
        try {
            if (inviteCode == null || inviteCode.trim().isEmpty()) {
                System.err.println("Le code d'invitation ne peut pas être vide");
                return false;
            }

            // Trouver le groupe par code d'invitation
            Group group = findGroupByInviteCode(inviteCode.trim());
            if (group == null) {
                System.err.println("Code d'invitation invalide");
                return false;
            }

            if (!group.isActive()) {
                System.err.println("Le groupe n'est pas actif");
                return false;
            }

            if (group.isMember(userId)) {
                System.err.println("L'utilisateur est déjà membre");
                return false;
            }

            // Ajouter l'utilisateur au groupe
            return addMemberToGroup(group.getGroupId(), userId, userId);

        } catch (Exception e) {
            System.err.println("Erreur lors de la jointure au groupe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mettre à jour les informations d'un groupe
     * @param groupId ID du groupe
     * @param newName Nouveau nom (optionnel)
     * @param newDescription Nouvelle description (optionnel)
     * @param newImageUrl Nouvelle URL d'image (optionnel)
     * @param updatedBy Utilisateur effectuant les modifications
     * @return true si mis à jour avec succès
     */
    public boolean updateGroupInfo(String groupId, String newName, String newDescription,
                                   String newImageUrl, String updatedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null || !group.canEditGroup(updatedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(2);
            final boolean[] success = {false};

            Map<String, Object> groupUpdates = new HashMap<>();
            Map<String, Object> chatUpdates = new HashMap<>();

            if (newName != null && !newName.trim().isEmpty()) {
                groupUpdates.put("groupName", newName.trim());
                chatUpdates.put("name", newName.trim());
            }

            if (newDescription != null) {
                groupUpdates.put("description", newDescription.trim());
            }

            if (newImageUrl != null) {
                groupUpdates.put("groupImageUrl", newImageUrl);
                chatUpdates.put("chatImageUrl", newImageUrl);
            }

            groupUpdates.put("lastActivity", ServerValue.TIMESTAMP);

            // Mettre à jour le groupe
            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(groupUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    success[0] = true;
                } else {
                    System.err.println("Erreur lors de la mise à jour des infos du groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            // Mettre à jour le chat correspondant
            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + groupId);
            chatRef.updateChildren(chatUpdates, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    System.err.println("Erreur lors de la mise à jour des infos du chat: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des infos du groupe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mettre à jour les paramètres d'un groupe
     * @param groupId ID du groupe
     * @param settings Nouveaux paramètres
     * @param updatedBy Utilisateur effectuant les modifications
     * @return true si mis à jour avec succès
     */
    public boolean updateGroupSettings(String groupId, GroupSettings settings, String updatedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null || !group.canEditGroup(updatedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            Map<String, Object> updates = new HashMap<>();
            updates.put("settings/onlyAdminsCanAdd", settings.isOnlyAdminsCanAdd());
            updates.put("settings/onlyAdminsCanMessage", settings.isOnlyAdminsCanMessage());
            updates.put("settings/allowMembersToInvite", settings.isAllowMembersToInvite());
            updates.put("settings/showMemberList", settings.isShowMemberList());
            updates.put("settings/allowFileSharing", settings.isAllowFileSharing());
            updates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(updates, (databaseError, databaseReference) -> {
                success[0] = (databaseError == null);
                if (databaseError != null) {
                    System.err.println("Erreur lors de la mise à jour des paramètres: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des paramètres: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprimer/désactiver un groupe
     * @param groupId ID du groupe
     * @param deletedBy Utilisateur demandant la suppression (doit être créateur)
     * @return true si supprimé avec succès
     */
    public boolean deleteGroup(String groupId, String deletedBy) {
        try {
            Group group = loadGroup(groupId);
            if (group == null || !group.isCreator(deletedBy)) {
                return false;
            }

            CountDownLatch latch = new CountDownLatch(2);
            final boolean[] success = {false};

            // Désactiver le groupe
            Map<String, Object> groupUpdates = new HashMap<>();
            groupUpdates.put("isActive", false);
            groupUpdates.put("lastActivity", ServerValue.TIMESTAMP);

            DatabaseReference groupRef = firebaseService.getDatabase().getReference("groups/" + groupId);
            groupRef.updateChildren(groupUpdates, (databaseError, databaseReference) -> {
                if (databaseError == null) {
                    success[0] = true;
                } else {
                    System.err.println("Erreur lors de la désactivation du groupe: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            // Désactiver le chat correspondant
            Map<String, Object> chatUpdates = new HashMap<>();
            chatUpdates.put("isActive", false);

            DatabaseReference chatRef = firebaseService.getDatabase().getReference("chats/" + groupId);
            chatRef.updateChildren(chatUpdates, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    System.err.println("Erreur lors de la désactivation du chat: " + databaseError.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);

            if (success[0]) {
                // Envoyer un message système final
                String deletedByName = getUserDisplayName(deletedBy);
                sendSystemMessage(groupId, "Le groupe a été supprimé par " + deletedByName);
            }

            return success[0];

        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du groupe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rechercher des groupes par nom
     * @param searchQuery Terme de recherche
     * @param currentUserId ID de l'utilisateur actuel (pour exclure les groupes privés)
     * @return Liste des groupes correspondants
     */
    public List<Group> searchGroups(String searchQuery, String currentUserId) {
        try {
            List<Group> results = new ArrayList<>();

            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                return results;
            }

            CountDownLatch latch = new CountDownLatch(1);
            DatabaseReference groupsRef = firebaseService.getDatabase().getReference("groups");

            groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                        try {
                            Group group = convertMapToGroup(groupSnapshot);

                            if (group != null && group.isActive()) {
                                String groupName = group.getGroupName();
                                String description = group.getDescription();

                                // Vérifier si la requête de recherche correspond au nom ou à la description
                                boolean nameMatches = groupName != null &&
                                        groupName.toLowerCase().contains(searchQuery.toLowerCase());
                                boolean descriptionMatches = description != null &&
                                        description.toLowerCase().contains(searchQuery.toLowerCase());

                                if (nameMatches || descriptionMatches) {
                                    // Si le groupe est public ou l'utilisateur est membre, ajouter aux résultats
                                    if (group.isPublic() || group.isMember(currentUserId)) {
                                        results.add(group);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur lors de l'analyse d'un groupe: " + e.getMessage());
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Erreur lors de la recherche: " + databaseError.getMessage());
                    latch.countDown();
                }
            });

            latch.await(15, TimeUnit.SECONDS);

            // Trier par nombre de membres (les plus populaires d'abord)
            results.sort((g1, g2) -> Integer.compare(g2.getMembers().size(), g1.getMembers().size()));

            return results;

        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche de groupes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Vérifier si un utilisateur peut envoyer des messages dans un groupe
     * @param groupId ID du groupe
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur peut envoyer des messages
     */
    public boolean canUserSendMessages(String groupId, String userId) {
        try {
            Group group = loadGroup(groupId);
            if (group != null) {
                return group.canSendMessages(userId);
            }
            return false;

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification des permissions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mettre à jour le nom du groupe
     * @param groupId ID du groupe
     * @param newName Nouveau nom du groupe
     * @param userId ID de l'utilisateur effectuant la modification (doit être admin)
     * @return true si la mise à jour a réussi
     */
    public boolean updateGroupName(String groupId, String newName, String userId) {
        if (groupId == null || newName == null || newName.trim().isEmpty() || userId == null) {
            return false;
        }

        return updateGroupInfo(groupId, newName, null, null, userId);
    }

    // Méthodes auxiliaires privées

    /**
     * Trouver un groupe par code d'invitation
     */
    private Group findGroupByInviteCode(String inviteCode) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final Group[] result = {null};

            DatabaseReference groupsRef = firebaseService.getDatabase().getReference("groups");
            groupsRef.orderByChild("inviteCode").equalTo(inviteCode)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                                result[0] = convertMapToGroup(groupSnapshot);
                                break; // Prendre la première correspondance
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("Erreur recherche par code d'invitation: " + databaseError.getMessage());
                            latch.countDown();
                        }
                    });

            latch.await(10, TimeUnit.SECONDS);
            return result[0];

        } catch (Exception e) {
            System.err.println("Erreur recherche par code d'invitation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Envoyer un message système au groupe
     */
    private void sendSystemMessage(String chatId, String content) {
        try {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("userId", "system");
            messageData.put("text", content);
            messageData.put("timestamp", ServerValue.TIMESTAMP);
            messageData.put("type", "SYSTEM");
            messageData.put("read", true);

            DatabaseReference messagesRef = firebaseService.getDatabase()
                    .getReference("chats/" + chatId + "/messages");
            messagesRef.push().setValueAsync(messageData);

            // Mettre à jour le dernier message du chat
            chatService.updateChatLastMessage(chatId, content, System.currentTimeMillis());

        } catch (Exception e) {
            System.err.println("Erreur envoi message système: " + e.getMessage());
        }
    }

    /**
     * Ajouter un chat à la liste de chats de chaque utilisateur
     */
    private void addChatToUsers(String chatId, List<String> userIds) {
        try {
            for (String userId : userIds) {
                DatabaseReference userChatsRef = firebaseService.getDatabase()
                        .getReference("users/" + userId + "/chats");
                userChatsRef.child(chatId).setValueAsync(true);
            }
        } catch (Exception e) {
            System.err.println("Erreur ajout chat aux utilisateurs: " + e.getMessage());
        }
    }

    // Méthodes de conversion de données

    private Map<String, Object> convertGroupToMap(Group group) {
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupId", group.getGroupId());
        groupData.put("groupName", group.getGroupName());
        groupData.put("description", group.getDescription());
        groupData.put("groupImageUrl", group.getGroupImageUrl());
        groupData.put("createdBy", group.getCreatedBy());
        groupData.put("createdAt", group.getCreatedAt());
        groupData.put("admins", group.getAdmins());
        groupData.put("members", group.getMembers());
        groupData.put("maxMembers", group.getMaxMembers());
        groupData.put("isPublic", group.isPublic());
        groupData.put("inviteCode", group.getInviteCode());
        groupData.put("isActive", group.isActive());
        groupData.put("lastActivity", group.getLastActivity());

        // Convertir les paramètres
        Map<String, Object> settingsMap = new HashMap<>();
        GroupSettings settings = group.getSettings();
        settingsMap.put("onlyAdminsCanAdd", settings.isOnlyAdminsCanAdd());
        settingsMap.put("onlyAdminsCanMessage", settings.isOnlyAdminsCanMessage());
        settingsMap.put("allowMembersToInvite", settings.isAllowMembersToInvite());
        settingsMap.put("showMemberList", settings.isShowMemberList());
        settingsMap.put("allowFileSharing", settings.isAllowFileSharing());
        groupData.put("settings", settingsMap);

        // Convertir les rôles des membres
        Map<String, String> memberRolesMap = new HashMap<>();
        for (String member : group.getMembers()) {
            memberRolesMap.put(member, group.getUserRole(member).name());
        }
        groupData.put("memberRoles", memberRolesMap);

        return groupData;
    }

    private Map<String, Object> convertChatToMap(Chat chat) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("users", chat.getParticipants());
        chatData.put("createdAt", chat.getCreatedAt());
        chatData.put("createdBy", chat.getCreatedBy());
        chatData.put("name", chat.getChatName());
        chatData.put("lastMessageText", chat.getLastMessage());
        chatData.put("lastMessageTime", chat.getLastMessageTime());
        chatData.put("isActive", chat.isActive());
        chatData.put("chatImageUrl", chat.getChatImageUrl());
        return chatData;
    }

    private Group convertMapToGroup(DataSnapshot dataSnapshot) {
        try {
            Group group = new Group();

            group.setGroupId(dataSnapshot.child("groupId").getValue(String.class));
            group.setGroupName(dataSnapshot.child("groupName").getValue(String.class));
            group.setDescription(dataSnapshot.child("description").getValue(String.class));
            group.setGroupImageUrl(dataSnapshot.child("groupImageUrl").getValue(String.class));
            group.setCreatedBy(dataSnapshot.child("createdBy").getValue(String.class));

            Long createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
            group.setCreatedAt(createdAt != null ? createdAt : 0);

            // Charger les membres
            List<String> members = new ArrayList<>();
            DataSnapshot membersSnapshot = dataSnapshot.child("members");
            if (membersSnapshot.exists()) {
                for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                    String member = memberSnapshot.getValue(String.class);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }
            group.setMembers(members);

            // Charger les admins
            List<String> admins = new ArrayList<>();
            DataSnapshot adminsSnapshot = dataSnapshot.child("admins");
            if (adminsSnapshot.exists()) {
                for (DataSnapshot adminSnapshot : adminsSnapshot.getChildren()) {
                    String admin = adminSnapshot.getValue(String.class);
                    if (admin != null) {
                        admins.add(admin);
                    }
                }
            }
            group.setAdmins(admins);

            // Charger les paramètres
            GroupSettings settings = new GroupSettings();
            DataSnapshot settingsSnapshot = dataSnapshot.child("settings");
            if (settingsSnapshot.exists()) {
                Boolean onlyAdminsCanAdd = settingsSnapshot.child("onlyAdminsCanAdd").getValue(Boolean.class);
                settings.setOnlyAdminsCanAdd(onlyAdminsCanAdd != null ? onlyAdminsCanAdd : false);

                Boolean onlyAdminsCanMessage = settingsSnapshot.child("onlyAdminsCanMessage").getValue(Boolean.class);
                settings.setOnlyAdminsCanMessage(onlyAdminsCanMessage != null ? onlyAdminsCanMessage : false);

                Boolean allowMembersToInvite = settingsSnapshot.child("allowMembersToInvite").getValue(Boolean.class);
                settings.setAllowMembersToInvite(allowMembersToInvite != null ? allowMembersToInvite : true);

                Boolean showMemberList = settingsSnapshot.child("showMemberList").getValue(Boolean.class);
                settings.setShowMemberList(showMemberList != null ? showMemberList : true);

                Boolean allowFileSharing = settingsSnapshot.child("allowFileSharing").getValue(Boolean.class);
                settings.setAllowFileSharing(allowFileSharing != null ? allowFileSharing : true);
            }
            group.setSettings(settings);

            // Charger les autres propriétés
            Integer maxMembers = dataSnapshot.child("maxMembers").getValue(Integer.class);
            group.setMaxMembers(maxMembers != null ? maxMembers : 100);

            Boolean isPublic = dataSnapshot.child("isPublic").getValue(Boolean.class);
            group.setPublic(isPublic != null ? isPublic : false);

            group.setInviteCode(dataSnapshot.child("inviteCode").getValue(String.class));

            Boolean isActive = dataSnapshot.child("isActive").getValue(Boolean.class);
            group.setActive(isActive != null ? isActive : true);

            Long lastActivity = dataSnapshot.child("lastActivity").getValue(Long.class);
            group.setLastActivity(lastActivity != null ? lastActivity : 0);

            return group;

        } catch (Exception e) {
            System.err.println("Erreur conversion map->group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Nettoie le nom du groupe pour l'utiliser comme ID Firebase
     */
    private String sanitizeGroupName(String groupName) {
        // Remplacer les caractères non autorisés par des underscores
        String sanitized = groupName
                .replaceAll("[.#$\\[\\]/]", "_")  // Caractères interdits Firebase
                .replaceAll("\\s+", "_")          // Espaces -> underscores
                .toLowerCase();                   // Minuscules pour consistance
        
        // Limiter la longueur pour éviter les IDs trop longs
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        // S'assurer qu'il commence par une lettre
        if (!sanitized.matches("^[a-z].*")) {
            sanitized = "group_" + sanitized;
        }
        
        return sanitized;
    }
    
    /**
     * Vérifie si un groupe avec ce nom existe déjà
     */
    private boolean groupExists(String groupId) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] exists = {false};
            
            DatabaseReference groupRef = firebaseService.getDatabase()
                    .getReference("groups/" + groupId);
            
            groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    exists[0] = dataSnapshot.exists();
                    latch.countDown();
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    latch.countDown();
                }
            });
            
            latch.await(5, TimeUnit.SECONDS);
            return exists[0];
            
        } catch (Exception e) {
            System.err.println("Erreur vérification existence groupe: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère le nom d'affichage d'un utilisateur (prénom + nom)
     */
    private String getUserDisplayName(String userId) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] displayName = {userId}; // Fallback to userId if name not found
            
            DatabaseReference userRef = firebaseService.getDatabase()
                    .getReference("users/" + userId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        
                        if (username == null || username.isEmpty()) {
                            // Si username n'est pas défini, utiliser prénom + nom
                            String nom = dataSnapshot.child("nom").getValue(String.class);
                            String prenom = dataSnapshot.child("prenom").getValue(String.class);
                            
                            if (prenom != null && nom != null) {
                                displayName[0] = prenom + " " + nom;
                            } else if (prenom != null) {
                                displayName[0] = prenom;
                            } else if (nom != null) {
                                displayName[0] = nom;
                            }
                        } else {
                            displayName[0] = username;
                        }
                    }
                    latch.countDown();
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Erreur récupération nom utilisateur: " + databaseError.getMessage());
                    latch.countDown();
                }
            });
            
            latch.await(5, TimeUnit.SECONDS);
            return displayName[0];
            
        } catch (Exception e) {
            System.err.println("Erreur getUserDisplayName: " + e.getMessage());
            return userId; // Return userId as fallback
        }
    }
}