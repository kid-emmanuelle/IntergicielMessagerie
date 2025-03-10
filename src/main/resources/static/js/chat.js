// Chat application
const chatApp = {
    // Configuration
    stompClient: null,
    username: null,
    connectionMode: null,
    currentReceiver: 'broadcast',
    isAnonymousMode: false,
    isDarkMode: false,
    isInfoPanelOpen: false,

    // DOM Elements
    userListElement: document.getElementById('userList'),
    messageListElement: document.getElementById('messageList'),
    messageForm: document.getElementById('messageForm'),
    messageInput: document.getElementById('messageInput'),
    fileListElement: document.getElementById('fileList'),
    noFilesMessage: document.getElementById('noFilesMessage'),
    fileUploadArea: document.getElementById('fileUploadArea'),
    darkModeToggle: document.getElementById('darkModeToggle'),
    infoPanel: document.querySelector('.info-panel'),

    // Initialize chat application
    init: function () {
        // Get username and mode from session storage
        this.username = sessionStorage.getItem('chat-username');
        this.connectionMode = sessionStorage.getItem('chat-mode');

        // Check if user is logged in
        if (!this.username) {
            window.location.href = '/login';
            return;
        }

        // Ensure DOM references are properly established after page load
        this.userListElement = document.getElementById('userList');
        this.messageListElement = document.getElementById('messageList');
        this.fileListElement = document.getElementById('fileList');

        // Set user initials
        const userInitials = document.getElementById('userInitials');
        if (userInitials) {
            userInitials.textContent = this.getInitials(this.username);
        }

        // Update UI with user info
        document.getElementById('usernameDisplay').textContent = this.username;

        const modeDisplay = document.getElementById('modeDisplay');
        if (this.connectionMode === 'PUBLIC') {
            modeDisplay.textContent = 'PUBLIC';
            modeDisplay.classList.add('public-mode');
            this.isAnonymousMode = false;
        } else {
            modeDisplay.textContent = 'ANONYMOUS';
            modeDisplay.classList.add('pseudo-anonymous');
            this.isAnonymousMode = true;
        }

        // Initialize dark mode from localStorage
        this.initDarkMode();

        // Set event listeners
        this.setEventListeners();

        // Connect to WebSocket
        this.connect();

        // Load user list - this should populate the Users tab
        this.loadOnlineUsers();

        // Show welcome message by default instead of loading broadcast messages
        this.showWelcomeMessage();

        // Check for pending files if in PUBLIC mode
        if (!this.isAnonymousMode) {
            this.loadPendingFiles();
        }
    },

    // Add a new method to show welcome message
    showWelcomeMessage: function () {
        // Clear message list
        this.messageListElement.innerHTML = '';

        // Add welcome message
        const welcomeMessage = document.createElement('div');
        welcomeMessage.className = 'welcome-message';
        welcomeMessage.innerHTML = `
        <i class="bi bi-chat-square-dots"></i>
        <p class="fw-bold">Welcome to OceaNous's Chat</p>
        <p>Select a user from the sidebar or use broadcast to start chatting</p>
    `;
        this.messageListElement.appendChild(welcomeMessage);

        // Reset current receiver to null to indicate no active chat
        this.currentReceiver = null;

        // Update header to show default info
        this.updateChatHeaderDefault();

        // Make sure the message input is disabled
        this.messageInput.disabled = true;
        document.getElementById('sendButton').disabled = true;
        this.messageInput.placeholder = "Select a conversation to start chatting";
    },

    // Update header to default state
    updateChatHeaderDefault: function () {
        const headerName = document.querySelector('.chat-header-name');
        const headerStatus = document.querySelector('.chat-header-status');
        const headerAvatar = document.querySelector('.chat-header-avatar span');

        headerName.textContent = 'Welcome to OceaNous';
        headerStatus.textContent = '28 members';
        headerAvatar.textContent = 'WO';
    },

    // Get initials from name
    getInitials: function (name) {
        if (!name) return '';
        return name.split(' ')
            .map(part => part.charAt(0))
            .join('')
            .toUpperCase()
            .substring(0, 2);
    },

    // Initialize dark mode state
    initDarkMode: function () {
        // Check localStorage for saved preference
        const savedDarkMode = localStorage.getItem('chat-dark-mode');
        if (savedDarkMode === 'true') {
            this.enableDarkMode();
        } else {
            this.disableDarkMode();
        }
    },

    // Enable dark mode
    enableDarkMode: function () {
        document.body.classList.add('dark-mode');
        this.darkModeToggle.classList.add('dark');
        this.isDarkMode = true;
        localStorage.setItem('chat-dark-mode', 'true');
    },

    // Disable dark mode
    disableDarkMode: function () {
        document.body.classList.remove('dark-mode');
        this.darkModeToggle.classList.remove('dark');
        this.isDarkMode = false;
        localStorage.setItem('chat-dark-mode', 'false');
    },

    // Toggle dark mode
    toggleDarkMode: function () {
        if (this.isDarkMode) {
            this.disableDarkMode();
        } else {
            this.enableDarkMode();
        }
    },

    // Toggle info panel
    toggleInfoPanel: function () {
        this.isInfoPanelOpen = !this.isInfoPanelOpen;
        if (this.isInfoPanelOpen) {
            this.infoPanel.classList.add('active');
        } else {
            this.infoPanel.classList.remove('active');
        }
    },

    // Set event listeners
    setEventListeners: function () {
        // Message form submission
        this.messageForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.sendMessage();
        });

        // Logout button
        document.getElementById('logoutBtn').addEventListener('click', () => {
            this.disconnect();
        });

        // Upload file button
        const uploadButton = document.getElementById('uploadButton');
        if (uploadButton) {
            uploadButton.addEventListener('click', () => {
                this.uploadFile();
            });
        }

        // Attachment button
        const attachmentButton = document.querySelector('.btn-attachment');
        if (attachmentButton) {
            attachmentButton.addEventListener('click', () => {
                document.getElementById('fileInput').click();
            });
        }

        // File input change
        const fileInput = document.getElementById('fileInput');
        if (fileInput) {
            fileInput.addEventListener('change', () => {
                this.handleFileSelection();
            });
        }

        // Select broadcast
        const broadcastItem = document.querySelector('[data-user="broadcast"]');
        if (broadcastItem) {
            broadcastItem.addEventListener('click', () => {
                this.selectReceiver('broadcast');
            });
        }

        // Info panel toggle
        const infoButton = document.querySelector('.chat-header-actions .bi-info-circle');
        if (infoButton) {
            infoButton.closest('button').addEventListener('click', () => {
                this.toggleInfoPanel();
            });
        }

        // Close info panel
        const closeInfoButton = document.querySelector('.btn-close-panel');
        if (closeInfoButton) {
            closeInfoButton.addEventListener('click', () => {
                this.toggleInfoPanel();
            });
        }

        // Setup dark mode toggle separately
        this.setupDarkModeToggle();

        // De-highlight files tab when clicked
        const filesTab = document.getElementById('files-tab');
        if (filesTab) {
            filesTab.addEventListener('click', () => {
                filesTab.classList.remove('text-primary');
            });
        }

        // Cancel file upload
        const cancelUploadButton = document.querySelector('.cancel-upload');
        if (cancelUploadButton) {
            cancelUploadButton.addEventListener('click', () => {
                this.cancelFileUpload();
            });
        }
    },

    // Handle file selection
    handleFileSelection: function () {
        const fileInput = document.getElementById('fileInput');
        const file = fileInput.files[0];

        if (file) {
            // Show file upload area
            this.fileUploadArea.classList.remove('d-none');

            // Update file name
            const fileNameElement = document.querySelector('.file-name');
            if (fileNameElement) {
                fileNameElement.textContent = file.name;
            }
        }
    },

    // Cancel file upload
    cancelFileUpload: function () {
        const fileInput = document.getElementById('fileInput');
        fileInput.value = '';
        this.fileUploadArea.classList.add('d-none');
    },

    // Set up dark mode toggle
    setupDarkModeToggle: function () {
        this.darkModeToggle = document.getElementById('darkModeToggle');
        if (this.darkModeToggle) {
            this.darkModeToggle.addEventListener('click', () => {
                this.toggleDarkMode();
            });
        }
    },

    // Connect to WebSocket
    connect: function () {
        // Add username to connection URL
        const socket = new SockJS('/ws-chat?username=' + encodeURIComponent(this.username));
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);

            // Subscribe to broadcast messages
            this.stompClient.subscribe('/topic/broadcast', (message) => {
                this.receiveBroadcastMessage(JSON.parse(message.body));
            });

            // If in PUBLIC mode, subscribe to private messages and file events
            if (!this.isAnonymousMode) {
                this.stompClient.subscribe('/user/queue/messages', (message) => {
                    this.receivePrivateMessage(JSON.parse(message.body));
                });

                this.stompClient.subscribe('/user/queue/files', (message) => {
                    this.receiveFileEvent(JSON.parse(message.body));
                });
            }

            // Subscribe to user status updates
            this.stompClient.subscribe('/topic/users', (message) => {
                this.updateUserStatus(JSON.parse(message.body));
            });

            // Send connection message
            this.stompClient.send('/app/chat.connect', {}, JSON.stringify({
                username: this.username,
                mode: this.connectionMode
            }));

            // Update UI based on connection mode
            if (this.isAnonymousMode) {
                // In anonymous mode, only allow viewing broadcast messages
                this.messageInput.disabled = true;
                document.getElementById('sendButton').disabled = true;
                this.fileUploadArea.classList.add('d-none');
                const attachButton = document.querySelector('.btn-attachment');
                if (attachButton) attachButton.classList.add('d-none');
            } else {
                // In public mode, enable messaging
                this.messageInput.disabled = false;
                document.getElementById('sendButton').disabled = false;
            }
        });
    },

    // Disconnect from WebSocket
    disconnect: function () {
        if (this.stompClient !== null) {
            // Send disconnect via REST API
            fetch('/api/users/disconnect', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({username: this.username})
            })
                .then(() => {
                    this.stompClient.disconnect();
                    sessionStorage.removeItem('chat-username');
                    sessionStorage.removeItem('chat-mode');
                    window.location.href = '/login';
                });
        }
    },

    // Add chat to the chat list
    addChatToList: function (chat) {
        const chatList = document.querySelector('.chat-list');
        if (!chatList) return;

        const chatItem = document.createElement('div');
        chatItem.className = 'chat-list-item';
        chatItem.setAttribute('data-chat', chat.username);

        const isOnline = chat.online;
        const initials = this.getInitials(chat.username);

        chatItem.innerHTML = `
            <div class="chat-avatar">
                <span>${initials}</span>
                ${isOnline ? '<span class="status-indicator online"></span>' : '<span class="status-indicator offline"></span>'}
            </div>
            <div class="chat-info">
                <div class="chat-name">${chat.username}</div>
                <div class="chat-last-message">${isOnline ? 'Online' : 'Offline'}</div>
            </div>
        `;

        chatItem.addEventListener('click', () => {
            this.selectReceiver(chat.username, isOnline);
        });

        chatList.appendChild(chatItem);
    },

    // Update user status
    updateUserStatus: function (userStatus) {
        // Skip self-updates
        if (userStatus.username === this.username) {
            return;
        }

        // Update in users panel (for backward compatibility)
        const existingUser = this.userListElement.querySelector(`[data-user="${userStatus.username}"]`);

        if (userStatus.online && userStatus.connectionMode === 'PUBLIC') {
            // User came online
            if (!existingUser) {
                this.addUserToList(userStatus);
            }
        } else {
            // User went offline or changed to PSEUDO_ANONYMOUS
            if (existingUser) {
                // Delete user from list
                existingUser.remove();
            }
        }

        // Update in chat list
        const chatItem = document.querySelector(`.chat-list-item[data-chat="${userStatus.username}"]`);
        if (chatItem) {
            const statusIndicator = chatItem.querySelector('.status-indicator');
            const lastMessage = chatItem.querySelector('.chat-last-message');

            if (userStatus.online) {
                if (statusIndicator) statusIndicator.classList.remove('offline');
                if (statusIndicator) statusIndicator.classList.add('online');
                if (lastMessage) lastMessage.textContent = 'Online';
            } else {
                if (statusIndicator) statusIndicator.classList.remove('online');
                if (statusIndicator) statusIndicator.classList.add('offline');
                if (lastMessage) lastMessage.textContent = 'Offline';
            }
        }

        // If user is the current receiver, update the message input state
        if (this.currentReceiver === userStatus.username) {
            if (userStatus.online) {
                // Enable messaging if user came online
                this.messageInput.disabled = this.isAnonymousMode;
                document.getElementById('sendButton').disabled = this.isAnonymousMode;
                this.messageInput.placeholder = "Type a message...";

                // Show attachment button
                if (!this.isAnonymousMode) {
                    const attachButton = document.querySelector('.btn-attachment');
                    if (attachButton) attachButton.classList.remove('d-none');
                }
            } else {
                // Disable messaging if user went offline
                this.messageInput.disabled = true;
                document.getElementById('sendButton').disabled = true;
                this.messageInput.placeholder = "User is offline. Messages cannot be sent.";

                // Hide attachment button
                const attachButton = document.querySelector('.btn-attachment');
                if (attachButton) attachButton.classList.add('d-none');
            }

            // Update header status
            this.updateChatHeader(userStatus.username, userStatus.online);
        }
    },

    // Load online users (keeping for backward compatibility)
    loadOnlineUsers: function () {
        fetch('/api/users/online')
            .then(response => response.json())
            .then(users => {
                // Make sure we have a reference to the user list element
                this.userListElement = document.getElementById('userList');
                if (!this.userListElement) {
                    console.error('User list element not found');
                    return;
                }

                // Preserve the broadcast item
                const existingBroadcast = this.userListElement.querySelector('[data-user="broadcast"]');
                this.userListElement.innerHTML = '';

                if (existingBroadcast) {
                    this.userListElement.appendChild(existingBroadcast);
                } else {
                    // If broadcast element was lost, recreate it
                    const broadcastItem = document.createElement('div');
                    broadcastItem.className = 'user-item';
                    broadcastItem.setAttribute('data-user', 'broadcast');
                    broadcastItem.innerHTML = `
                    <div class="user-avatar">
                        <i class="bi bi-broadcast"></i>
                    </div>
                    <div class="user-info">
                        <span class="user-name">Broadcast to All</span>
                    </div>
                `;
                    broadcastItem.addEventListener('click', () => {
                        this.selectReceiver('broadcast');
                    });
                    this.userListElement.appendChild(broadcastItem);
                }

                // Add other users
                users.forEach(user => {
                    if (user.username !== this.username && user.connectionMode === 'PUBLIC') {
                        this.addUserToList(user);
                    }
                });

                console.log(`Loaded ${users.length} online users`);
            })
            .catch(error => {
                console.error('Error loading online users:', error);
            });
    },

    // Add user to list (keeping for backward compatibility)
    addUserToList: function (user) {
        const userItem = document.createElement('div');
        userItem.className = 'user-item';
        userItem.setAttribute('data-user', user.username);

        const initials = this.getInitials(user.username);

        userItem.innerHTML = `
            <div class="user-avatar">
                <span>${initials}</span>
            </div>
            <div class="user-info">
                <span class="user-name">${user.username}</span>
            </div>
        `;

        userItem.addEventListener('click', () => {
            this.selectReceiver(user.username, user.online);
        });

        this.userListElement.appendChild(userItem);
    },

    // Clear unread badge for a user
    clearUnreadBadge: function (username) {
        const userElement = this.userListElement.querySelector(`[data-user="${username}"]`);
        if (userElement) {
            userElement.classList.remove('unread');
            const badge = userElement.querySelector('.user-badge');
            if (badge) {
                badge.classList.add('d-none');
                badge.textContent = '';
            }
        }
    },

    // Select receiver for messaging
    selectReceiver: function (username) {
        // Clear active user
        const activeUser = this.userListElement.querySelector('.active');
        if (activeUser) {
            activeUser.classList.remove('active');
        }

        // Set new active user
        const userElement = this.userListElement.querySelector(`[data-user="${username}"]`);
        if (userElement) {
            userElement.classList.add('active');
        }

        this.currentReceiver = username;
        this.messageListElement.innerHTML = '';

        // Clear unread badge when selecting a user
        this.clearUnreadBadge(username);

        // Show file upload for private messages in PUBLIC mode
        if (username !== 'broadcast' && !this.isAnonymousMode) {
            document.querySelector('.btn-attachment').classList.remove('d-none');
        } else {
            document.querySelector('.btn-attachment').classList.add('d-none');
        }

        // Update header with receiver info
        this.updateChatHeader(username);

        // Load appropriate message history
        if (username === 'broadcast') {
            this.loadBroadcastHistory();
        } else {
            this.loadPrivateMessageHistory(username);
        }

        // Focus on message input
        this.messageInput.focus();
    },

    // Update chat header with receiver info
    updateChatHeader: function (username) {
        const headerName = document.querySelector('.chat-header-name');
        const headerStatus = document.querySelector('.chat-header-status');
        const headerAvatar = document.querySelector('.chat-header-avatar span');

        if (username === 'broadcast') {
            headerName.textContent = 'Broadcast';
            headerStatus.textContent = 'Message to all users';
            headerAvatar.textContent = 'BC';
        } else {
            headerName.textContent = username;
            headerStatus.textContent = 'Online';
            headerAvatar.textContent = this.getInitials(username);
        }
    },

    // Load broadcast message history
    loadBroadcastHistory: function () {
        fetch('/api/messages/broadcast')
            .then(response => response.json())
            .then(messages => {
                this.messageListElement.innerHTML = '';

                // Add date separator
                this.addDateSeparator('Today');

                messages.forEach(message => {
                    this.displayMessage(message, true);
                });
                this.scrollToBottom();
            });
    },

    // Load private message history
    loadPrivateMessageHistory: function (otherUser) {
        fetch(`/api/messages/private?user1=${this.username}&user2=${otherUser}`)
            .then(response => response.json())
            .then(messages => {
                this.messageListElement.innerHTML = '';

                // Add date separator
                this.addDateSeparator('Today');

                messages.forEach(message => {
                    this.displayMessage(message, false);
                });
                this.scrollToBottom();
            });
    },

    // Add date separator
    addDateSeparator: function (dateText) {
        const separator = document.createElement('div');
        separator.className = 'message-date-separator';
        separator.innerHTML = `<span>${dateText}</span>`;
        this.messageListElement.appendChild(separator);
    },

    // Send a message
    sendMessage: function () {
        const content = this.messageInput.value.trim();
        if (!content) return;

        if (this.currentReceiver === 'broadcast') {
            // Send broadcast message
            this.stompClient.send('/app/chat.broadcast', {}, JSON.stringify({
                content: content
            }));
        } else {
            // Send private message
            this.stompClient.send('/app/chat.private', {}, JSON.stringify({
                receiver: this.currentReceiver,
                content: content
            }));
        }

        this.messageInput.value = '';
        this.messageInput.focus();
    },

    // Display a message in the chat area
    displayMessage: function (message, isBroadcast) {
        const isCurrentUser = message.sender === this.username;
        const messageElement = document.createElement('div');

        messageElement.className = isCurrentUser ? 'message message-sent' : 'message message-received';

        // Format timestamp
        const timestamp = new Date(message.timestamp);
        const formattedTime = timestamp.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});

        let messageHtml = '';

        // For received messages, add the header with sender name and time
        if (!isCurrentUser) {
            messageHtml += `
            <div class="message-header">
                <span class="message-sender">${message.sender}</span>
                <span class="message-time">${formattedTime}</span>
            </div>
        `;
        }

        // Add the content container that holds the avatar and bubble
        messageHtml += `<div class="message-content-container">`;

        // Add avatar for received messages
        if (!isCurrentUser) {
            const initials = this.getInitials(message.sender);
            messageHtml += `
            <div class="message-avatar">
                <span>${initials}</span>
            </div>
        `;
        }

        // Start the message bubble
        messageHtml += `<div class="message-bubble">`;

        // Check if it's a regular message or a file attachment
        if (message.type !== 'BINARY' || !message.fileInfo) {
            // Regular text message
            messageHtml += `<span class="message-body">${message.content}</span>`;
        } else {
            // File attachment
            const filename = message.fileInfo.filename;
            const fileId = message.fileInfo.id;
            const receiver = message.receiver || '';

            // Check if it's an image file
            const isImage = this.isImageFile(filename);

            messageHtml += `<div class="file-attachment">`;
            messageHtml += `<div class="file-attachment-text">File attachment: ${filename}</div>`;

            // Standard download button for non-image files
            messageHtml += `
                <button class="file-download-btn" onclick="chatApp.downloadFile(${fileId}, '${receiver}')">
                    <i class="bi bi-download"></i> Download
                </button>
            `;

            messageHtml += `</div>`;
        }

        // Close the message bubble and content container
        messageHtml += `</div></div>`;

        // For sent messages, add the time at the bottom after the bubble
        if (isCurrentUser) {
            messageHtml += `<span class="message-time">${formattedTime}</span>`;
        }

        messageElement.innerHTML = messageHtml;
        this.messageListElement.appendChild(messageElement);
        this.scrollToBottom();
    },

    // Helper function to check if a file is an image
    isImageFile: function (filename) {
        const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];
        const ext = filename.toLowerCase().substring(filename.lastIndexOf('.'));
        return imageExtensions.includes(ext);
    },

    // Format file size for display
    formatFileSize: function (bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    },

    // Receive broadcast message
    receiveBroadcastMessage: function (message) {
        // Always display broadcast messages if they're new
        this.displayMessage(message, true);

        // If not currently viewing broadcast, mark unread in sidebar
        if (this.currentReceiver !== 'broadcast') {
            const chatListItem = document.querySelector('.chat-list-item[data-chat="broadcast"]');
            if (chatListItem) {
                const badge = chatListItem.querySelector('.chat-badge');
                if (badge) {
                    const count = parseInt(badge.textContent) || 0;
                    badge.textContent = count + 1;
                    badge.classList.remove('d-none');
                }
            }
        }
    },

    // Receive private message
    receivePrivateMessage: function (message) {
        const sender = message.sender;

        // If currently chatting with this user, display message
        if (this.currentReceiver === message.sender || message.sender === this.username) {
            this.displayMessage(message, false);
        }

        // If not currently viewing conversation with this sender, mark as unread
        if (this.currentReceiver !== message.sender && message.sender !== this.username) {
            const userElement = this.userListElement.querySelector(`[data-user="${message.sender}"]`);
            if (userElement) {
                userElement.classList.add('unread');

                // Add visual indicator
                const badge = userElement.querySelector('.user-badge');
                if (badge) {
                    const count = parseInt(badge.textContent) || 0;
                    badge.textContent = count + 1;
                    badge.classList.remove('d-none');
                } else {
                    const userInfo = userElement.querySelector('.user-info');
                    const badge = document.createElement('span');
                    badge.className = 'user-badge';
                    badge.textContent = '1';
                    userInfo.appendChild(badge);
                }
            }
        }
    },

    // Upload a file
    uploadFile: function () {
        const fileInput = document.getElementById('fileInput');
        const file = fileInput.files[0];

        if (!file) {
            alert('Please select a file to upload');
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            alert('File size exceeds the limit of 10MB');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);
        formData.append('sender', this.username);
        formData.append('receiver', this.currentReceiver);

        fetch('/api/files/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (response.ok) {
                    fileInput.value = '';
                    this.fileUploadArea.classList.add('d-none');
                    console.log('File uploaded successfully');
                } else {
                    alert('Error uploading file');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error uploading file');
            });
    },

    // Download a file
    downloadFile: function (fileId, username) {
        window.location.href = `/api/files/download/${fileId}?username=${username}`;
    },

    // Load pending files
    loadPendingFiles: function () {
        fetch(`/api/files/pending?username=${this.username}`)
            .then(response => response.json())
            .then(files => {
                this.fileListElement.innerHTML = '';

                if (files.length === 0) {
                    this.noFilesMessage.classList.remove('d-none');
                } else {
                    this.noFilesMessage.classList.add('d-none');

                    files.forEach(file => {
                        const fileElement = document.createElement('div');
                        fileElement.className = 'card mb-2';
                        fileElement.innerHTML = `
                            <div class="card-body">
                                <h6 class="card-title">
                                    <i class="bi bi-file-earmark me-2"></i>${file.filename}
                                </h6>
                                <div class="card-text">
                                    <div class="file-info-item">
                                        <i class="bi bi-person me-2"></i>From: ${file.sender}
                                    </div>
                                    <div class="file-info-item">
                                        <i class="bi bi-hdd me-2"></i>Size: ${this.formatFileSize(file.fileSize)}
                                    </div>
                                    <div class="file-info-item">
                                        <i class="bi bi-clock me-2"></i>Expires: ${new Date(file.expiryTime).toLocaleString()}
                                    </div>
                                </div>
                                <div class="mt-2">
                                    <button class="btn btn-sm btn-primary"
                                        onclick="chatApp.downloadFile(${file.id}, '${this.username}')">
                                        <i class="bi bi-download me-1"></i>Download
                                    </button>
                                </div>
                            </div>
                        `;

                        this.fileListElement.appendChild(fileElement);
                    });
                }
            });
    },

    // Receive file event notification
    receiveFileEvent: function (fileEvent) {
        // Update file list if viewing files tab
        this.loadPendingFiles();

        // Show notification based on event type
        switch (fileEvent.eventType) {
            case 'NEW_FILE_AVAILABLE':
                // Highlight files tab
                document.getElementById('files-tab').classList.add('text-primary');

                // Notification
                this.showNotification(`New file from ${fileEvent.sender}`, 'file');
                break;

            case 'FILE_DOWNLOADED':
                // Update files tab to show download occurred
                this.loadPendingFiles();
                break;

            case 'FILE_EXPIRED':
                // Remove from pending files
                this.loadPendingFiles();
                break;
        }
    },

    // Show a notification
    showNotification: function (message, type) {
        // Check if browser notifications are supported
        if (!("Notification" in window)) {
            console.log("This browser does not support desktop notifications");
            return;
        }

        // Check if permission is already granted
        if (Notification.permission === "granted") {
            this.createNotification(message, type);
        }
        // Otherwise, request permission
        else if (Notification.permission !== "denied") {
            Notification.requestPermission().then(permission => {
                if (permission === "granted") {
                    this.createNotification(message, type);
                }
            });
        }
    },

    // Create a notification
    createNotification: function (message, type) {
        const icon = type === 'message' ? '/img/message-icon.png' : '/img/file-icon.png';
        const notification = new Notification("OceaNous's Chat", {
            body: message,
            icon: icon
        });

        // Close notification after 5 seconds
        setTimeout(() => {
            notification.close();
        }, 5000);

        // Handle notification click
        notification.onclick = () => {
            window.focus();
            notification.close();
        };
    },

    // Scroll chat to bottom
    scrollToBottom: function () {
        this.messageListElement.scrollTop = this.messageListElement.scrollHeight;
    }
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    chatApp.init();
});