// ChitChat Backend MongoDB Database Initialization Script
// Database: chitchat
// Username: summitcodeworks
// Password: 8ivhaah8

// Switch to chitchat database
db = db.getSiblingDB('chitchat');

// Create collections with validation schemas

// Messages collection
db.createCollection("messages", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["senderId", "content", "type", "status"],
      properties: {
        senderId: { bsonType: "long" },
        recipientId: { bsonType: "long" },
        groupId: { bsonType: "string" },
        content: { bsonType: "string" },
        type: { 
          enum: ["TEXT", "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "LOCATION", "CONTACT", "STICKER"]
        },
        status: { 
          enum: ["SENT", "DELIVERED", "READ", "FAILED"]
        },
        mediaUrl: { bsonType: "string" },
        thumbnailUrl: { bsonType: "string" },
        replyToMessageId: { bsonType: "string" },
        mentions: { 
          bsonType: "array",
          items: { bsonType: "string" }
        },
        scheduledAt: { bsonType: "date" },
        deliveredAt: { bsonType: "date" },
        readAt: { bsonType: "date" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" }
      }
    }
  }
});

// Groups collection
db.createCollection("groups", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name", "adminId", "members"],
      properties: {
        name: { bsonType: "string" },
        description: { bsonType: "string" },
        avatarUrl: { bsonType: "string" },
        adminId: { bsonType: "long" },
        members: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["userId", "role"],
            properties: {
              userId: { bsonType: "long" },
              role: { 
                enum: ["ADMIN", "MODERATOR", "MEMBER"]
              },
              joinedAt: { bsonType: "date" },
              lastSeen: { bsonType: "date" }
            }
          }
        },
        settings: {
          bsonType: "object",
          properties: {
            allowMembersToInvite: { bsonType: "bool" },
            allowMembersToChangeGroupInfo: { bsonType: "bool" },
            allowMembersToSendMessages: { bsonType: "bool" },
            allowMembersToSendMedia: { bsonType: "bool" },
            groupDescription: { bsonType: "string" }
          }
        },
        lastActivity: { bsonType: "date" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" }
      }
    }
  }
});

// Statuses collection
db.createCollection("statuses", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "type", "privacy"],
      properties: {
        userId: { bsonType: "long" },
        content: { bsonType: "string" },
        mediaUrl: { bsonType: "string" },
        thumbnailUrl: { bsonType: "string" },
        type: { 
          enum: ["TEXT", "IMAGE", "VIDEO", "AUDIO"]
        },
        privacy: { 
          enum: ["PUBLIC", "CONTACTS_ONLY", "SELECTED_CONTACTS"]
        },
        expiresAt: { bsonType: "date" },
        views: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["userId", "viewedAt"],
            properties: {
              userId: { bsonType: "long" },
              viewedAt: { bsonType: "date" }
            }
          }
        },
        reactions: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["userId", "emoji", "reactedAt"],
            properties: {
              userId: { bsonType: "long" },
              emoji: { bsonType: "string" },
              reactedAt: { bsonType: "date" }
            }
          }
        },
        lastActivity: { bsonType: "date" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" }
      }
    }
  }
});

// Create indexes for better performance

// Messages indexes
db.messages.createIndex({ "senderId": 1 });
db.messages.createIndex({ "recipientId": 1 });
db.messages.createIndex({ "groupId": 1 });
db.messages.createIndex({ "createdAt": -1 });
db.messages.createIndex({ "status": 1 });
db.messages.createIndex({ "type": 1 });
db.messages.createIndex({ "senderId": 1, "recipientId": 1, "createdAt": -1 });
db.messages.createIndex({ "groupId": 1, "createdAt": -1 });

// Groups indexes
db.groups.createIndex({ "adminId": 1 });
db.groups.createIndex({ "members.userId": 1 });
db.groups.createIndex({ "lastActivity": -1 });
db.groups.createIndex({ "createdAt": -1 });
db.groups.createIndex({ "name": "text", "description": "text" });

// Statuses indexes
db.statuses.createIndex({ "userId": 1 });
db.statuses.createIndex({ "createdAt": -1 });
db.statuses.createIndex({ "expiresAt": 1 });
db.statuses.createIndex({ "type": 1 });
db.statuses.createIndex({ "privacy": 1 });
db.statuses.createIndex({ "lastActivity": -1 });
db.statuses.createIndex({ "userId": 1, "createdAt": -1 });

// Create TTL index for statuses (auto-delete expired statuses)
db.statuses.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

// Create compound indexes for common queries
db.messages.createIndex({ 
  "senderId": 1, 
  "recipientId": 1, 
  "createdAt": -1 
});

db.messages.createIndex({ 
  "groupId": 1, 
  "createdAt": -1 
});

db.statuses.createIndex({ 
  "userId": 1, 
  "expiresAt": 1 
});

// Insert sample data (optional - for testing)

// Sample group
db.groups.insertOne({
  "_id": ObjectId(),
  "name": "ChitChat Team",
  "description": "Official ChitChat development team",
  "avatarUrl": null,
  "adminId": 1,
  "members": [
    {
      "userId": 1,
      "role": "ADMIN",
      "joinedAt": new Date(),
      "lastSeen": new Date()
    }
  ],
  "settings": {
    "allowMembersToInvite": true,
    "allowMembersToChangeGroupInfo": false,
    "allowMembersToSendMessages": true,
    "allowMembersToSendMedia": true,
    "groupDescription": "Official development team group"
  },
  "lastActivity": new Date(),
  "createdAt": new Date(),
  "updatedAt": new Date()
});

// Sample status
db.statuses.insertOne({
  "_id": ObjectId(),
  "userId": 1,
  "content": "Welcome to ChitChat! 🎉",
  "mediaUrl": null,
  "thumbnailUrl": null,
  "type": "TEXT",
  "privacy": "PUBLIC",
  "expiresAt": new Date(Date.now() + 24 * 60 * 60 * 1000), // 24 hours from now
  "views": [],
  "reactions": [],
  "lastActivity": new Date(),
  "createdAt": new Date(),
  "updatedAt": new Date()
});

print("MongoDB database 'chitchat' initialized successfully!");
print("Collections created: messages, groups, statuses");
print("Indexes created for optimal performance");
print("Sample data inserted for testing");
